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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import java.util.List;

/**
 * Normalizes array creations for wasm.
 *
 * <p>Ater this pass is run, all array creations are in one of 3 forms:
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
    // Implementing ArrayLiterals will introduced new NewArray nodes in the ast. We need first to
    // process all arrayLiterals and then we can normalize multi-dimensional array creation.
    removeExplicitInstantiationInArrayLiterals(compilationUnit);
    normalizeMultidimensionalArrays(compilationUnit);
  }

  /**
   * Replaces array instantiations that have literals with the literal itself.
   *
   * <p>After this rewriting arrays are either an explicit creation with dimension expressions, e.g.
   * {@code new Array[4][]} or array literals e.g. {@code \{\{1,3\},null\}}.
   */
  private static void removeExplicitInstantiationInArrayLiterals(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteNewArray(NewArray newArray) {
            Expression initializer = newArray.getInitializer();
            if (initializer == null) {
              return newArray;
            }
            checkState(initializer instanceof ArrayLiteral);
            return initializer;
          }
        });
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
   * <p>where 5 is the id for the pritimive type int and -1 is used to denote an uninitialized array
   * dimension.
   */
  private static void normalizeMultidimensionalArrays(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteNewArray(NewArray newArray) {
            if (newArray.getDimensionExpressions().size() < 2) {
              return newArray;
            }
            checkArgument(newArray.getInitializer() == null);

            List<Expression> nonNullDimensions =
                newArray.getDimensionExpressions().stream()
                    .map(NormalizeArrayCreationsWasm::nullToMinusOne)
                    .collect(toImmutableList());

            return RuntimeMethods.createCreateMultiDimensionalArrayCall(
                new ArrayLiteral(
                    ArrayTypeDescriptor.newBuilder()
                        .setComponentTypeDescriptor(PrimitiveTypes.INT)
                        .build(),
                    nonNullDimensions),
                NumberLiteral.fromInt(getTypeIndex(newArray.getLeafTypeDescriptor())));
          }
        });
  }

  private static int getTypeIndex(TypeDescriptor typeDescriptor) {
    return PrimitiveTypes.TYPES.indexOf(typeDescriptor);
  }

  private static Expression nullToMinusOne(Expression original) {
    return original instanceof NullLiteral ? NumberLiteral.fromInt(-1) : original;
  }
}
