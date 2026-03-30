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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import java.util.ArrayList;
import java.util.List;

/**
 * Normalizes array creations for wasm.
 *
 * <p>After this pass is run, all array creations are in one of 3 forms:
 *
 * <ul>
 *   <li>an unidimensional array creation, e.g. {@code new String[3]}
 *   <li>an array literal, e.g. {@code {1,2}}. (note that the components in array literals are
 *       expressions and if they were array creation they would be in one of these three forms
 *   <li>a call to the runtime to create a multidimensional array).
 * </ul>
 */
public class NormalizeArrayCreationsWasm extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // Expand native JsType array literals first since their handling introduces NewArray
    // expressions that must be also rewritten.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteArrayLiteral(ArrayLiteral arrayLiteral) {
            if (!arrayLiteral.getTypeDescriptor().isNativeJsArray()) {
              return arrayLiteral;
            }
            return expandNativeJsTypeArrayLiterals(arrayLiteral);
          }
        });

    // Rewrite NewArray expressions.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteNewArray(NewArray newArray) {
            if (newArray.getInitializer() != null) {
              return implementArrayCreationFromArrayLiteral(newArray);
            } else {
              return implementArrayCreationWithDimensions(newArray);
            }
          }
        });
  }

  /**
   * Replaces array instantiations that have literals with the literal itself.
   *
   * <p>After this rewriting arrays are either an explicit creation with dimension expressions, e.g.
   * {@code new Array[4][]} or array literals e.g. {@code \{\{1,3\},null\}}.
   */
  private static Expression implementArrayCreationFromArrayLiteral(NewArray newArray) {
    Expression initializer = newArray.getInitializer();
    checkState(initializer instanceof ArrayLiteral || initializer instanceof MultiExpression);
    return initializer;
  }

  /**
   * Rewrite all explicit multidimensional array creations with a call to the runtime to create a
   * multidimensional initialized array.
   *
   * <p>This pass will rewrite expressions like
   *
   * <pre>{@code
   * new int[2][3][]
   * }</pre>
   *
   * <p>to
   *
   * <pre>{@code
   * WasmArray.createMultiDimensionalArray([2,3,-1], 5)
   * }</pre>
   *
   * <p>where 5 is the id for the primitive type int and -1 is used to denote an uninitialized array
   * dimension.
   */
  private static Expression implementArrayCreationWithDimensions(NewArray newArray) {
    if (newArray.getDimensionExpressions().size() == 1) {
      if (newArray.getTypeDescriptor().isNativeJsArray()) {
        // WasmExtern.createArray(size)
        return MethodCall.Builder.from(
                TypeDescriptors.get()
                    .javaemulInternalWasmExtern
                    .getMethodDescriptorByName("createArray"))
            .setArguments(newArray.getDimensionExpressions().getFirst())
            .build();
      }
      return newArray;
    }

    checkArgument(newArray.getInitializer() == null);

    Expression dimensionsParameter =
        ArrayLiteral.newBuilder()
            .setTypeDescriptor(
                ArrayTypeDescriptor.newBuilder()
                    .setComponentTypeDescriptor(PrimitiveTypes.INT)
                    .build())
            .setValueExpressions(
                newArray.getDimensionExpressions().stream()
                    .map(NormalizeArrayCreationsWasm::nullToMinusOne)
                    .collect(toImmutableList()))
            .build();

    if (newArray.getTypeDescriptor().isNativeJsArray()) {
      // WasmExtern.createMultiDimensionalArray(new int[] { d1, d2 })
      return MethodCall.Builder.from(
              TypeDescriptors.get()
                  .javaemulInternalWasmExtern
                  .getMethodDescriptorByName("createMultiDimensionalArray"))
          .setArguments(dimensionsParameter)
          .build();
    }

    return RuntimeMethods.createCreateMultiDimensionalArrayCall(
        dimensionsParameter, NumberLiteral.fromInt(getTypeIndex(newArray.getLeafTypeDescriptor())));
  }

  private static int getTypeIndex(TypeDescriptor typeDescriptor) {
    return PrimitiveTypes.TYPES.indexOf(typeDescriptor);
  }

  private static Expression nullToMinusOne(Expression original) {
    return original instanceof NullLiteral ? NumberLiteral.fromInt(-1) : original;
  }

  private static Expression expandNativeJsTypeArrayLiterals(ArrayLiteral arrayLiteral) {
    // Create a simple native array. We can ignore dimensions since:
    // 1. There is no metadata to represent multidimensional arrays, they are just arrays of
    // arrays.
    // 2. Since it has array literal, nested array creations are taken care of by their
    // corresponding expansion.
    int size = arrayLiteral.getValueExpressions().size();
    NewArray newArray = newNativeJsTypeArrayCall(size);
    if (size == 0) {
      return newArray;
    }

    // Wasm lacks a literal initialization construct for external JS objects.
    // We simulate array literals by sequentially setting its elements.
    List<Expression> expressions = new ArrayList<>();

    // var nativeArray = new Array(size)
    Variable variable =
        Variable.newBuilder()
            .setName("nativeArray")
            .setTypeDescriptor(getWasmExernArrayType())
            .setFinal(true)
            .build();
    expressions.add(
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclaration(variable, newArray)
            .build());

    // tmp[0] = literal_0
    // tmp[1] = literal_1
    // ...
    for (int i = 0; i < size; i++) {
      expressions.add(
          BinaryExpression.Builder.asAssignmentTo(
                  ArrayAccess.newBuilder()
                      .setArrayExpression(variable.createReference())
                      .setIndexExpression(NumberLiteral.fromInt(i))
                      .build())
              .setRightOperand(arrayLiteral.getValueExpressions().get(i))
              .build());
    }

    // return tmp
    expressions.add(variable.createReference());
    return MultiExpression.newBuilder().setExpressions(expressions).build();
  }

  private static NewArray newNativeJsTypeArrayCall(int size) {
    // new Array(size)
    return NewArray.newBuilder()
        .setTypeDescriptor(getWasmExernArrayType())
        .setDimensionExpressions(ImmutableList.of(NumberLiteral.fromInt(size)))
        .setInitializer(null)
        .build();
  }

  private static ArrayTypeDescriptor getWasmExernArrayType() {
    return ArrayTypeDescriptor.newBuilder()
        .setComponentTypeDescriptor(TypeDescriptors.get().javaemulInternalWasmExtern)
        .build();
  }
}
