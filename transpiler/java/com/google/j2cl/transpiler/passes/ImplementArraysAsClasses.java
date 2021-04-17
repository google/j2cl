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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.ArrayLength;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.Type;
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
    normalizeArrays(compilationUnit);
  }

  private void markNativeWasmArrayTypes(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public boolean shouldProcessType(Type type) {
            // We do not expect any access to the native arrays outside of these class.
            return TypeDescriptors.get()
                .javaemulInternalWasmArray
                .equals(type.getSuperTypeDescriptor());
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
                .setTargetFieldDescriptor(markFieldTypeDescriptorAsNative(fieldAccess.getTarget()))
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
        });
  }

  private static void normalizeArrays(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteNewArray(NewArray newArray) {
            if (newArray.getTypeDescriptor().isNativeWasmArray()) {
              return newArray;
            }

            checkState(newArray.getDimensionExpressions().size() == 1);

            return NewInstance.Builder.from(
                    TypeDescriptors.getWasmArrayType(newArray.getTypeDescriptor())
                        .getMethodDescriptor("<init>", PrimitiveTypes.INT))
                .setArguments(newArray.getDimensionExpressions().get(0))
                .build();
          }

          @Override
          public Node rewriteArrayLength(ArrayLength arrayLength) {
            ArrayTypeDescriptor arrayTypeDescriptor =
                (ArrayTypeDescriptor) arrayLength.getArrayExpression().getTypeDescriptor();
            if (arrayTypeDescriptor.isNativeWasmArray()) {
              return arrayLength;
            }

            return FieldAccess.Builder.from(
                    TypeDescriptors.get().javaemulInternalWasmArray.getFieldDescriptor("length"))
                .setQualifier(arrayLength.getArrayExpression())
                .build();
          }

          @Override
          public Node rewriteArrayAccess(ArrayAccess arrayAccess) {
            ArrayTypeDescriptor arrayTypeDescriptor =
                (ArrayTypeDescriptor) arrayAccess.getArrayExpression().getTypeDescriptor();
            if (arrayTypeDescriptor.isNativeWasmArray()) {
              return arrayAccess;
            }

            return ArrayAccess.Builder.from(arrayAccess)
                .setArrayExpression(getInnerNativeArrayExpression(arrayAccess.getArrayExpression()))
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
    return ArrayTypeDescriptor.Builder.from(original).setNativeWasmArray(true).build();
  }

  private static Expression getInnerNativeArrayExpression(Expression arrayExpression) {
    checkState(arrayExpression.getTypeDescriptor().isArray());

    return FieldAccess.newBuilder()
        .setQualifier(arrayExpression)
        .setTargetFieldDescriptor(
            markFieldTypeDescriptorAsNative(
                TypeDescriptors.getWasmArrayType(
                        (ArrayTypeDescriptor) arrayExpression.getTypeDescriptor())
                    .getFieldDescriptor("elements")))
        .build();
  }
}
