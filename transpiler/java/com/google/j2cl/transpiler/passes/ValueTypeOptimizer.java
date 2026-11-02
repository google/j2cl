/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.JsConstructorReference;
import com.google.j2cl.transpiler.ast.JsDocExpression;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Helper to optimize value-like classes using {@code ValueType} to reduce code size. */
final class ValueTypeOptimizer {
  /**
   * Returns all instance fields declared on {@code type} and its supertypes up to {@code
   * java.lang.Object}.
   */
  static List<FieldDescriptor> getAllInstanceFields(TypeDeclaration type) {
    List<FieldDescriptor> fields = new ArrayList<>();
    for (TypeDeclaration current = type;
        !TypeDescriptors.isJavaLangObject(current.toRawTypeDescriptor());
        current = current.getSuperTypeDeclaration()) {
      Iterables.addAll(fields, getInstanceFields(current));
    }
    return fields;
  }

  static Iterable<FieldDescriptor> getInstanceFields(TypeDeclaration type) {
    return Iterables.filter(type.getDeclaredFieldDescriptors(), FieldDescriptor::isInstanceMember);
  }

  static int getMethodMask(MethodDescriptor method) {
    checkArgument(method.isOrOverridesJavaLangObjectMethod(), method);
    return switch (method.getName()) {
      case "equals" -> 1;
      case "hashCode" -> 2;
      case "toString" -> 4;
      default -> throw new AssertionError(method);
    };
  }

  static void optimizeAsValueType(
      Type type, int mask, Collection<FieldDescriptor> excludedFields) {
    if (mask == 0) {
      // No method needs optimization. Leave the type alone.
      return;
    }

    if (TypeDescriptors.isJavaLangObject(type.getSuperTypeDescriptor())) {
      // Change the parent type of the type to ValueType.
      type.setSuperTypeDescriptor(TypeDescriptors.get().javaemulInternalValueType);
      updateSuperCall(type.getPrimaryConstructor(), type.getImplicitSuperCallTarget());
      // Record excluded fields if there are any.
      if (!excludedFields.isEmpty()) {
        addExcludedFieldsDeclaration(type, excludedFields);
      }
    } else {
      // Add mixin that will provide the implementation for java.lang.Object.
      addValueTypeMixin(type, mask, excludedFields);
    }

    // Prevent JsCompiler from optimizing the fields to not break the semantics.
    // If we don't take any special precautions, if a field is never read explicitly (even
    // if it is set), JsCompiler can optimize them away. As a result they won't be seen
    // reflectively and they won't contribute to equals/hashCode/toString.
    AstUtils.preserveFields(type, excludedFields);
  }

  /** Updates {@code constructor}'s super() call to target {@code superConstructor}. */
  static void updateSuperCall(Method constructor, MethodDescriptor superConstructor) {
    ExpressionStatement statement = AstUtils.getConstructorInvocationStatement(constructor);
    // We only inline AutoValue classes that have trivial constructors.
    // This guarantees their existing super() calls have no custom arguments, making it
    // safe to blindly retarget them as implicit, parameterless super calls to ValueType().
    checkState(
        statement.getExpression() instanceof MethodCall methodCall
            && methodCall.getArguments().isEmpty());
    MethodCall superCall =
        MethodCall.builderFrom(superConstructor)
            .setArguments(AstUtils.maybePackageVarargs(superConstructor, ImmutableList.of()))
            .build();
    List<Statement> statements = constructor.getBody().getStatements();
    statements.set(
        statements.indexOf(statement), superCall.makeStatement(statement.getSourcePosition()));
  }

  private static void addValueTypeMixin(
      Type type, int mask, Collection<FieldDescriptor> excludedFields) {
    MethodDescriptor mixinMethodDescriptor =
        TypeDescriptors.get().javaemulInternalValueType.getMethodDescriptorByName("mixin");

    MethodCall mixinCall =
        MethodCall.builderFrom(mixinMethodDescriptor)
            .setArguments(
                new JsConstructorReference(type.getDeclaration()),
                new JsConstructorReference(
                    TypeDescriptors.get().javaemulInternalValueType.getTypeDeclaration()),
                NumberLiteral.fromInt(mask),
                getPropertyNameExpressions(type.getDeclaration(), excludedFields))
            .build();
    type.addLoadTimeStatement(mixinCall.makeStatement(SourcePosition.NONE));
  }

  private static void addExcludedFieldsDeclaration(
      Type type, Collection<FieldDescriptor> excludedFields) {
    FieldDescriptor excludedFieldDescriptor =
        FieldDescriptor.builder()
            .setOriginalJsInfo(JsInfo.RAW_FIELD)
            .setEnclosingTypeDescriptor(TypeDescriptors.get().javaLangObject)
            .setTypeDescriptor(TypeDescriptors.get().javaLangObject)
            .setName("$excluded_fields")
            .build();
    Expression excludedFieldAccess = createPrototypeFieldAccess(type, excludedFieldDescriptor);

    // Property name expression is guarded by @pureOrBreakMyCode to make it movable by JsCompiler's
    // cross chunk code motion. Note that ValueType.mixin doesn't need this since JsCompiler is
    // treating class definition helpers separately.
    // TODO(b/435019132): Remove this annotation once the JsCompiler handles the pattern.
    Expression propertyNameExpressions =
        JsDocExpression.builder()
            .setAnnotation("pureOrBreakMyCode")
            .setExpression(getPropertyNameExpressions(type.getDeclaration(), excludedFields))
            .build();

    // Adds load time statement MyFoo.prototype.$excluded_fields = [ ... ]
    type.addLoadTimeStatement(
        excludedFieldAccess
            .infixAssign(propertyNameExpressions)
            .makeStatement(SourcePosition.NONE));
  }

  private static ArrayLiteral getPropertyNameExpressions(
      TypeDeclaration type, Collection<FieldDescriptor> fields) {
    // Note that ValueType.objectProperty is just a native proxy for goog.reflect.objectProperty.
    MethodCall.Builder objectPropertyCallBuilder =
        MethodCall.builderFrom(
            TypeDescriptors.get()
                .javaemulInternalValueType
                .getMethodDescriptorByName("objectProperty"));
    // Generates an array literal in the form of:
    // [
    //   ValueType.objectProperty("<property1>", MyType),
    //   ValueType.objectProperty("<property2>", MyType),
    //   ...
    // ]
    return ArrayLiteral.builder()
        .setTypeDescriptor(TypeDescriptors.get().javaLangObjectArray)
        .setValueExpressions(
            fields.stream()
                .map(FieldDescriptor::getMangledName)
                .map(StringLiteral::new)
                .map(
                    name ->
                        objectPropertyCallBuilder
                            .setArguments(name, new JsConstructorReference(type))
                            .build())
                .toArray(Expression[]::new))
        .build();
  }

  private static Expression createPrototypeFieldAccess(Type type, FieldDescriptor field) {
    return FieldAccess.builderFrom(field)
        .setQualifier(new JsConstructorReference(type.getDeclaration()).getPrototypeFieldAccess())
        .build();
  }

  private ValueTypeOptimizer() {}
}
