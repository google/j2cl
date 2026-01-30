/*
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.passes;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BindingPattern;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.PatternMatchExpression;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides implementations for Java record classes.
 *
 * <p>Example record class:
 *
 * <pre>{@code
 * record Foo(String a, int b) {}
 * }</pre>
 *
 * <p>The AST representation of a record contains only what is written in the source but not the
 * implicit fields and methods. The fields are present in the type model. We must generate the
 * following:
 *
 * <ul>
 *   <li>Field declarations for record fields in the AST.
 *   <li>Public field accessors for record fields.
 *   <li>A canonical constructor that takes all record fields as parameters.
 *   <li>For compact constructors, add field assignments to the body.
 *   <li>Implementations for Object methods, such as equals, hashCode, and toString.
 * </ul>
 */
public class ImplementRecordClasses extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    if (!type.isJavaRecord()) {
      return;
    }

    addFieldDeclarations(type);
    normalizeConstructors(type);
    addFieldAccessors(type);
    if (isValueTypeBasedImplementation(type)) {
      AstUtils.preserveFields(type, ImmutableList.of());
      return;
    }
    implementEquals(type);
    implementHashCode(type);
    implementToString(type);
  }

  private static boolean isValueTypeBasedImplementation(Type type) {
    DeclaredTypeDescriptor javaemulInternalValueType =
        TypeDescriptors.get().javaemulInternalValueType;
    return javaemulInternalValueType != null
        && type.getDeclaration().isSubtypeOf(javaemulInternalValueType.getTypeDeclaration());
  }

  /**
   * Adds field declarations for record fields.
   *
   * <p>Record fields are present in the type model but not in the AST.
   */
  private static void addFieldDeclarations(Type type) {
    for (FieldDescriptor field : getRecordFields(type.getTypeDescriptor())) {
      if (type.getInstanceFields().stream()
          .anyMatch(f -> f.getDescriptor().getName().equals(field.getName()))) {
        continue;
      }
      type.addMember(Field.Builder.from(field).setSourcePosition(type.getSourcePosition()).build());
    }
  }

  private static void addFieldAccessors(Type type) {
    for (FieldDescriptor field : getRecordFields(type.getTypeDescriptor())) {
      MethodDescriptor fieldAccessorDescriptor =
          MethodDescriptor.newBuilder()
              .setEnclosingTypeDescriptor(type.getTypeDescriptor())
              .setName(field.getName())
              .setReturnTypeDescriptor(field.getTypeDescriptor())
              .setSynthetic(true)
              .build();
      if (type.containsMethod(fieldAccessorDescriptor::isSameSignature)) {
        continue;
      }
      type.addMember(
          Method.newBuilder()
              .setMethodDescriptor(fieldAccessorDescriptor)
              .addStatements(
                  ReturnStatement.newBuilder()
                      .setExpression(
                          FieldAccess.Builder.from(field)
                              .setQualifier(new ThisReference(type.getTypeDescriptor()))
                              .build())
                      .setSourcePosition(SourcePosition.NONE)
                      .build())
              .setSourcePosition(type.getSourcePosition())
              .build());
    }
  }

  private static void normalizeConstructors(Type type) {
    MethodDescriptor canonicalConstructorDescriptor =
        MethodDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(type.getTypeDescriptor())
            .setConstructor(true)
            .setParameterDescriptors(
                getRecordFields(type.getTypeDescriptor()).stream()
                    .map(
                        f ->
                            MethodDescriptor.ParameterDescriptor.newBuilder()
                                .setTypeDescriptor(f.getTypeDescriptor())
                                .build())
                    .collect(toImmutableList()))
            .setSynthetic(true)
            .build();

    // Generate the canonical constructor if it does not exist.
    // That is, given a record like `record Foo(String a, int b)`, we expect that a canonical
    // constructor `Foo(String, int)` exists.
    if (!type.containsMethod(canonicalConstructorDescriptor::isSameSignature)) {
      type.addMember(
          Method.newBuilder()
              .setMethodDescriptor(canonicalConstructorDescriptor)
              .setParameters(
                  AstUtils.createParameterVariables(
                      canonicalConstructorDescriptor.getParameterTypeDescriptors()))
              .setSourcePosition(type.getSourcePosition())
              .build());
    }

    // Add field assignments to compact constructors.
    //
    // For a compact constructor, such as:
    // record Foo(String value) {
    //   Foo {
    //     // Example user-added logic:
    //     checkArgument(value != null);
    //   }
    // }
    //
    // The frontend doesn't generate assignments for us. Add assignments, resulting in:
    // record Foo(String value) {
    //   Foo {
    //     // Example user-added logic:
    //     checkArgument(value != null);
    //     this.value = value;
    //   }
    // }
    for (Method constructor : type.getConstructors()) {
      if (!isCompactConstructor(constructor)) {
        continue;
      }

      constructor.setBody(
          Block.Builder.from(constructor.getBody())
              .addStatements(
                  Streams.zip(
                          getRecordFields(type.getTypeDescriptor()).stream(),
                          constructor.getParameters().stream(),
                          (field, parameter) ->
                              BinaryExpression.Builder.asAssignmentTo(field)
                                  .setRightOperand(parameter.createReference())
                                  .build()
                                  .makeStatement(constructor.getSourcePosition()))
                      .collect(toImmutableList()))
              .build());
    }
  }

  private static void implementHashCode(Type type) {
    implementObjectMethodOverride(
        type,
        "hashCode",
        () -> RuntimeMethods.createArraysHashCodeMethodCall(createRecordFieldAccessList(type)));
  }

  private static void implementToString(Type type) {
    implementObjectMethodOverride(
        type,
        "toString",
        () -> RuntimeMethods.createArraysToStringMethodCall(createRecordFieldAccessList(type)));
  }

  private static void implementEquals(Type type) {
    implementObjectMethodOverride(
        type,
        "equals",
        parameters -> {
          Variable parameter = parameters.get(0);
          Variable otherVariable =
              Variable.newBuilder()
                  .setName("$other")
                  .setTypeDescriptor(type.getTypeDescriptor())
                  .build();
          return ImmutableList.<Statement>of(
              // if (!(other instanceof RecordClassType $other)) return false;
              IfStatement.newBuilder()
                  .setConditionExpression(
                      PatternMatchExpression.newBuilder()
                          .setExpression(parameter.createReference())
                          .setPattern(new BindingPattern(otherVariable))
                          .build()
                          .prefixNot())
                  .setThenStatement(
                      ReturnStatement.newBuilder()
                          .setExpression(BooleanLiteral.get(false))
                          .setSourcePosition(SourcePosition.NONE)
                          .build())
                  .setSourcePosition(SourcePosition.NONE)
                  .build(),
              // return Arrays.equals({this.a, this.b}, {$other.a, $other.b});
              ReturnStatement.newBuilder()
                  .setExpression(
                      RuntimeMethods.createArraysEqualsMethodCall(
                          createRecordFieldAccessList(type),
                          createRecordFieldAccessList(otherVariable.createReference())))
                  .setSourcePosition(SourcePosition.NONE)
                  .build());
        },
        TypeDescriptors.get().javaLangObject);
  }

  private static void implementObjectMethodOverride(
      Type type, String methodName, Supplier<Expression> returnExpression) {
    implementObjectMethodOverride(
        type,
        methodName,
        unusedParameters ->
            ImmutableList.of(
                ReturnStatement.newBuilder()
                    .setExpression(returnExpression.get())
                    .setSourcePosition(SourcePosition.NONE)
                    .build()));
  }

  private static void implementObjectMethodOverride(
      Type type,
      String methodName,
      Function<List<Variable>, List<Statement>> makeMethodStatements,
      TypeDescriptor... parameterTypes) {
    MethodDescriptor methodDescriptor =
        TypeDescriptors.get().javaLangObject.getMethodDescriptor(methodName, parameterTypes);
    if (type.containsMethod(m -> m.isOverride(methodDescriptor))) {
      return;
    }
    MethodDescriptor generatedMethodDescriptor =
        MethodDescriptor.Builder.from(methodDescriptor)
            .setEnclosingTypeDescriptor(type.getTypeDescriptor())
            .setDeclarationDescriptor(null)
            .setNative(false)
            .setSynthetic(true)
            .build();
    List<Variable> parameters =
        AstUtils.createParameterVariables(methodDescriptor.getParameterTypeDescriptors());
    type.addMember(
        Method.newBuilder()
            .setMethodDescriptor(generatedMethodDescriptor)
            .setParameters(parameters)
            .setSourcePosition(type.getSourcePosition())
            .setStatements(makeMethodStatements.apply(parameters))
            .build());
  }

  private static ImmutableList<Expression> createRecordFieldAccessList(Type recordType) {
    return createRecordFieldAccessList(new ThisReference(recordType.getTypeDescriptor()));
  }

  private static ImmutableList<Expression> createRecordFieldAccessList(Expression qualifier) {
    DeclaredTypeDescriptor recordTypeDescriptor =
        (DeclaredTypeDescriptor) qualifier.getTypeDescriptor();
    return getRecordFields(recordTypeDescriptor).stream()
        .map(field -> FieldAccess.Builder.from(field).setQualifier(qualifier.clone()).build())
        .collect(toImmutableList());
  }

  private static ImmutableList<FieldDescriptor> getRecordFields(
      DeclaredTypeDescriptor recordTypeDescriptor) {
    return recordTypeDescriptor.getDeclaredFieldDescriptors().stream()
        .filter(FieldDescriptor::isInstanceMember)
        .collect(toImmutableList());
  }

  private static boolean isCompactConstructor(Method constructor) {
    DeclaredTypeDescriptor enclosingTypeDescriptor =
        constructor.getDescriptor().getEnclosingTypeDescriptor();
    boolean[] isCompactConstructor = {true};
    // A compact constructor is the canonical constructor (no `this()` call) and doesn't have any
    // field assignments.
    constructor.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethodCall(MethodCall methodCall) {
            if (methodCall.getTarget().isConstructor()
                && methodCall.getTarget().isMemberOf(enclosingTypeDescriptor)) {
              isCompactConstructor[0] = false;
            }
          }

          @Override
          public void exitBinaryExpression(BinaryExpression binaryExpression) {
            if (binaryExpression.isSimpleOrCompoundAssignment()
                && binaryExpression.getLeftOperand() instanceof FieldAccess fieldAccess
                && fieldAccess.getTarget().isInstanceMember()
                && fieldAccess.getTarget().isMemberOf(enclosingTypeDescriptor)) {
              isCompactConstructor[0] = false;
            }
          }
        });
    return isCompactConstructor[0];
  }
}
