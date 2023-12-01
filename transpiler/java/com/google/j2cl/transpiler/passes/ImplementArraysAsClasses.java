/*
 * Copyright 2021 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.ArrayLength;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableReference;
import java.util.HashMap;
import java.util.Map;

/** Rewrite Arrays operations for Wasm */
public class ImplementArraysAsClasses extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    markNativeWasmArrayTypes(compilationUnit);
    normalizeArrayAccesses(compilationUnit);
    replaceArrayCreationsWithWasmArrayInstantiations(compilationUnit);
  }

  /**
   * Goes over all the array types declared in the Java array abstraction classes and marks them as
   * native wasm arrays, since in those classes all arrays that appear in the code are meant to be
   * the underlying native wasm array. Similarly we mark the parameters of all @Wasm methods to use
   * the native arrays as well.
   *
   * <p>Note that arrays declared in the WasmArray class are not native arrays.
   *
   * <p>TODO(b/203307171): Introduce a way to mark wasm types as native to remove this special
   * handling that makes it confusing when reasoning what gets rewritten and what does not.
   */
  private void markNativeWasmArrayTypes(CompilationUnit compilationUnit) {
    // Mark native wasm arrays in wasm array subtypes
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public boolean shouldProcessType(Type type) {
            // We do not expect any access to the native arrays outside of these classes.
            return TypeDescriptors.isWasmArraySubtype(type.getTypeDescriptor());
          }

          @Override
          public Node rewriteField(Field field) {
            if (!field.getDescriptor().getTypeDescriptor().isArray()) {
              return field;
            }
            return Field.Builder.from(field)
                .setDescriptor(markFieldTypeDescriptorAsNative(field.getDescriptor()))
                .build();
          }

          @Override
          public Node rewriteFieldAccess(FieldAccess fieldAccess) {
            if (!fieldAccess.getTypeDescriptor().isArray()) {
              return fieldAccess;
            }
            return FieldAccess.Builder.from(fieldAccess)
                .setTarget(markFieldTypeDescriptorAsNative(fieldAccess.getTarget()))
                .build();
          }

          private final Map<Variable, Variable> variableReplacements = new HashMap<>();

          @Override
          public Node rewriteVariable(Variable variable) {
            if (!variable.getTypeDescriptor().isArray()) {
              return variable;
            }
            Variable newVariable = markVariableTypeDescriptorAsNative(variable);
            variableReplacements.put(variable, newVariable);
            return newVariable;
          }

          @Override
          public Node rewriteVariableReference(VariableReference variableReference) {
            if (!variableReference.getTypeDescriptor().isArray()) {
              return variableReference;
            }
            return new VariableReference(variableReplacements.get(variableReference.getTarget()));
          }

          @Override
          public Expression rewriteNewArray(NewArray newArray) {
            return NewArray.Builder.from(newArray)
                .setTypeDescriptor(markArrayTypeDescriptorAsNative(newArray.getTypeDescriptor()))
                .build();
          }

          @Override
          public Expression rewriteArrayLiteral(ArrayLiteral arrayLiteral) {
            return new ArrayLiteral(
                markArrayTypeDescriptorAsNative(arrayLiteral.getTypeDescriptor()),
                arrayLiteral.getValueExpressions());
          }

          @Override
          public Node rewriteConditionalExpression(ConditionalExpression conditionalExpression) {
            if (!conditionalExpression.getTypeDescriptor().isArray()) {
              return conditionalExpression;
            }
            return ConditionalExpression.Builder.from(conditionalExpression)
                .setTypeDescriptor(
                    maybeMarkTypeDescriptorAsNative(conditionalExpression.getTypeDescriptor()))
                .build();
          }

          @Override
          public Expression rewriteCastExpression(CastExpression castExpression) {
            return CastExpression.Builder.from(castExpression)
                .setCastTypeDescriptor(
                    maybeMarkTypeDescriptorAsNative(castExpression.getCastTypeDescriptor()))
                .build();
          }
        });

    // Mark native wasm arrays in @Wasm methods.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteVariable(Variable variable) {
            if (variable.getTypeDescriptor().isArray() && isNativeMethodParameter()) {
              return markVariableTypeDescriptorAsNative(variable);
            }
            return variable;
          }

          private boolean isNativeMethodParameter() {
            return getParent() instanceof Method
                && ((Method) getParent()).getDescriptor().getWasmInfo() != null;
          }
        });
  }

  /**
   * Rewrites array operations to go throught the appropriate Java array abstraction type.
   *
   * <p>For array length, accesses the field {@code WasmArray.length}. For accesses to an array
   * element, it accesses the element through the native array field of the abstraction class, e.g.
   * {@code WasmArray.OfShort.elements[i]}
   */
  private static void normalizeArrayAccesses(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteArrayLength(ArrayLength arrayLength) {
            if (!isNonNativeArray(arrayLength.getArrayExpression().getTypeDescriptor())) {
              return arrayLength;
            }

            return ArrayLength.Builder.from(arrayLength)
                .setArrayExpression(getInnerNativeArrayExpression(arrayLength.getArrayExpression()))
                .build();
          }

          @Override
          public Node rewriteArrayAccess(ArrayAccess arrayAccess) {
            if (!isNonNativeArray(arrayAccess.getArrayExpression().getTypeDescriptor())) {
              return arrayAccess;
            }

            return ArrayAccess.Builder.from(arrayAccess)
                .setArrayExpression(getInnerNativeArrayExpression(arrayAccess.getArrayExpression()))
                .build();
          }

          @Override
          public MethodCall rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor target = methodCall.getTarget();
            if (target.getWasmInfo() == null) {
              return methodCall;
            }
            if (target.getParameterTypeDescriptors().stream().noneMatch(this::isNonNativeArray)) {
              return methodCall;
            }

            // TODO(goktug): Consider allowing native arrays on return types as well.
            return MethodCall.Builder.from(methodCall)
                .setArguments(
                    methodCall.getArguments().stream()
                        .map(
                            arg ->
                                needsNativeArray(methodCall, arg)
                                    ? getInnerNativeArrayExpression(arg)
                                    : arg)
                        .collect(toImmutableList()))
                .build();
          }

          private boolean isNonNativeArray(TypeDescriptor descriptor) {
            return descriptor instanceof ArrayTypeDescriptor
                && !((ArrayTypeDescriptor) descriptor).isNativeWasmArray();
          }

          private boolean needsNativeArray(MethodCall call, Expression expression) {
            return isNonNativeArray(
                call.getTarget()
                    .getParameterTypeDescriptors()
                    .get(call.getArguments().indexOf(expression)));
          }
        });
  }

  /**
   * Converts all array instatiations (NewArray and ArrayLiteral) to a call to the corresponding
   * WasmArray class constructor to create the Java array abstraction of the proper type.
   *
   * <p>At this point all arrays initialized with more than one explicit dimension have been already
   * normalized into method calls to the runtime that return the initialized array. The code
   * initializes such arrays via array creations that will be then converted by this pass as well.
   */
  private static void replaceArrayCreationsWithWasmArrayInstantiations(
      CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteNewArray(NewArray newArray) {
            if (newArray.getTypeDescriptor().isNativeWasmArray()) {
              return newArray;
            }

            checkState(newArray.getDimensionExpressions().size() == 1);

            return MethodCall.Builder.from(
                    TypeDescriptors.getWasmArrayType(newArray.getTypeDescriptor())
                        .getMethodDescriptor("newWithLength", PrimitiveTypes.INT))
                .setArguments(newArray.getDimensionExpressions().get(0))
                .build();
          }

          @Override
          public Expression rewriteArrayLiteral(ArrayLiteral arrayLiteral) {
            ArrayTypeDescriptor arrayTypeDescriptor = arrayLiteral.getTypeDescriptor();
            if (arrayTypeDescriptor.isNativeWasmArray()) {
              return arrayLiteral;
            }

            ArrayTypeDescriptor nativeArrayTypeDescriptor =
                markArrayTypeDescriptorAsNative(
                    arrayTypeDescriptor.getComponentTypeDescriptor().isPrimitive()
                        ? arrayTypeDescriptor
                        : TypeDescriptors.get().javaLangObjectArray);
            return MethodCall.Builder.from(
                    TypeDescriptors.getWasmArrayType(arrayTypeDescriptor)
                        .getMethodDescriptor("newWithLiteral", nativeArrayTypeDescriptor))
                .setArguments(
                    new ArrayLiteral(nativeArrayTypeDescriptor, arrayLiteral.getValueExpressions()))
                .build();
          }
        });
  }

  private static FieldDescriptor markFieldTypeDescriptorAsNative(FieldDescriptor original) {
    return FieldDescriptor.Builder.from(original)
        .setTypeDescriptor(
            markArrayTypeDescriptorAsNative((ArrayTypeDescriptor) original.getTypeDescriptor()))
        .build();
  }

  private static Variable markVariableTypeDescriptorAsNative(Variable original) {
    return Variable.Builder.from(original)
        .setTypeDescriptor(
            markArrayTypeDescriptorAsNative((ArrayTypeDescriptor) original.getTypeDescriptor()))
        .build();
  }

  private static ArrayTypeDescriptor markArrayTypeDescriptorAsNative(ArrayTypeDescriptor original) {
    return ArrayTypeDescriptor.Builder.from(original).setMarkedAsNativeWasmArray(true).build();
  }

  private static TypeDescriptor maybeMarkTypeDescriptorAsNative(TypeDescriptor original) {
    if (original.isArray()) {
      return markArrayTypeDescriptorAsNative((ArrayTypeDescriptor) original);
    }
    return original;
  }

  private static Expression getInnerNativeArrayExpression(Expression arrayExpression) {
    checkState(arrayExpression.getTypeDescriptor().isArray());

    return FieldAccess.newBuilder()
        .setQualifier(arrayExpression)
        .setTarget(
            markFieldTypeDescriptorAsNative(
                TypeDescriptors.getWasmArrayType(
                        (ArrayTypeDescriptor) arrayExpression.getTypeDescriptor())
                    .getFieldDescriptor("elements")))
        .build();
  }
}
