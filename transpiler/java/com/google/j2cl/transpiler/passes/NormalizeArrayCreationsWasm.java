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

import com.google.common.collect.Lists;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import java.util.List;
import java.util.stream.Collectors;

/** Normalizes array creations for wasm. */
public class NormalizeArrayCreationsWasm extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // Implementing ArrayLiterals will introduced new NewArray nodes in the ast. We need first to
    // process all arrayLiterals and then we can normalize multi-dimensional array creation.
    implementArrayLiterals(compilationUnit);
    normalizeMultidimensionalArrays(compilationUnit);
  }

  /**
   * Replaces array literals with explicit array creations, explicitly assigning the values from the
   * literals.
   */
  private static void implementArrayLiterals(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteNewArray(NewArray newArray) {
            if (newArray.getArrayLiteral() == null) {
              return newArray;
            }

            return createInitializedArrayExpression(
                newArray.getTypeDescriptor(), newArray.getArrayLiteral().getValueExpressions());
          }
        });
  }

  /**
   * Rewrite multidimensional array creation by a call to the helper method. This method returns an
   * one-dimensional array.
   */
  private static void normalizeMultidimensionalArrays(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteNewArray(NewArray newArray) {
            if (newArray.getDimensionExpressions().size() < 2) {
              return newArray;
            }
            checkArgument(newArray.getArrayLiteral() == null);

            List<Expression> nonNullDimensions =
                newArray.getDimensionExpressions().stream()
                    .map(NormalizeArrayCreationsWasm::nullToMinusOne)
                    .collect(Collectors.toList());

            return RuntimeMethods.createCreateMultiDimensionalArrayCall(
                createInitializedArrayExpression(
                    ArrayTypeDescriptor.newBuilder()
                        .setComponentTypeDescriptor(PrimitiveTypes.INT)
                        .build(),
                    nonNullDimensions),
                NumberLiteral.fromInt(getTypeIndex(newArray.getLeafTypeDescriptor())));
          }
        });
  }

  /**
   * Replaces array literals with explicit array creations, explicitly assigning the values from the
   * literals.
   *
   * <p>Ex: new int []{1, 2} -> ($array = new int[2], $array[0] = 1, $array[1] = 2, $array)
   *
   * <p>Multidimensional array literals are handled a dimension at a time.
   */
  private static Expression createInitializedArrayExpression(
      ArrayTypeDescriptor arrayTypeDescriptor, List<Expression> valueExpressions) {

    Variable arrayVariable =
        Variable.newBuilder()
            .setFinal(true)
            .setName("$array_literal")
            .setTypeDescriptor(arrayTypeDescriptor)
            .build();

    List<Expression> dimensions =
        Lists.newArrayList(NumberLiteral.fromInt(valueExpressions.size()));
    AstUtils.addNullPadding(dimensions, arrayTypeDescriptor.getDimensions());
    // Instantiate the array.
    // ex: new int []{1, 2} -> $array = new int[2]
    VariableDeclarationExpression arrayDeclaration =
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclaration(
                arrayVariable,
                NewArray.newBuilder()
                    .setDimensionExpressions(dimensions)
                    .setTypeDescriptor(arrayTypeDescriptor)
                    .build())
            .build();

    MultiExpression.Builder multiExpressionBuilder =
        MultiExpression.newBuilder().addExpressions(arrayDeclaration);

    // Add a set operations for every values of the array literal.
    // new int []{1, 2} ->  $array[0] = 1, $array[1] = 2
    for (int i = 0; i < valueExpressions.size(); i++) {
      Expression valueExpression = valueExpressions.get(i);
      multiExpressionBuilder.addExpressions(
          BinaryExpression.Builder.asAssignmentTo(
                  ArrayAccess.newBuilder()
                      .setArrayExpression(arrayVariable.createReference())
                      .setIndexExpression(NumberLiteral.fromInt(i))
                      .build())
              .setRightOperand(valueExpression)
              .build());
    }

    return multiExpressionBuilder.addExpressions(arrayVariable.createReference()).build();
  }

  private static int getTypeIndex(TypeDescriptor typeDescriptor) {
    return PrimitiveTypes.TYPES.indexOf(typeDescriptor);
  }

  private static Expression nullToMinusOne(Expression original) {
    return original instanceof NullLiteral ? NumberLiteral.fromInt(-1) : original;
  }
}
