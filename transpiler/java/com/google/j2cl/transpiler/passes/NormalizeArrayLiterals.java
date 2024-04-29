/*
 * Copyright 2015 Google Inc.
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

import com.google.common.collect.Lists;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrefixExpression;
import com.google.j2cl.transpiler.ast.PrefixOperator;
import com.google.j2cl.transpiler.ast.Type;
import java.util.List;

/**
 * Rewrites the rare short form array literal initializers (like "int[] foo = {1, 2, 3};") into the
 * more common long form (like "int[] foo = new int[] {1, 2, 3};").
 *
 * <p>Remove spread operator on an Array present in an array initializer: `new int[] {...new int[]
 * {1,2,3}} -> new int[] {1,2,3}
 */
public class NormalizeArrayLiterals extends NormalizationPass {

  @Override
  public void applyTo(Type type) {

    // Rewrite short form array literals
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteArrayLiteral(ArrayLiteral arrayLiteral) {
            if (getParent() instanceof NewArray) {
              return arrayLiteral;
            }

            return NewArray.newBuilder()
                .setTypeDescriptor(arrayLiteral.getTypeDescriptor())
                .setDimensionExpressions(
                    AstUtils.createListOfNullValues(
                        arrayLiteral.getTypeDescriptor().getDimensions()))
                .setInitializer(arrayLiteral)
                .build();
          }
        });

    // Remove spread operator on an Array inside an ArrayLiteral:
    // `new int[] {...new int[] {1,2,3}} -> new int[] {1,2,3}
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteNewArray(NewArray newArray) {
            if (!(newArray.getInitializer() instanceof ArrayLiteral)) {
              return newArray;
            }

            ArrayLiteral arrayLiteral = (ArrayLiteral) newArray.getInitializer();
            ArrayTypeDescriptor arrayTypeDescriptor = arrayLiteral.getTypeDescriptor();
            if (arrayLiteral.getValueExpressions().isEmpty()
                && !arrayTypeDescriptor.isUntypedArray()) {
              // Replace the empty literal with the explicit new Component[0].
              List<Expression> dimensions = Lists.newArrayList(NumberLiteral.fromInt(0));
              AstUtils.addNullPadding(dimensions, arrayTypeDescriptor.getDimensions());
              return NewArray.newBuilder()
                  .setTypeDescriptor(newArray.getTypeDescriptor())
                  .setDimensionExpressions(dimensions)
                  .setInitializer(null)
                  .build();
            }

            if (arrayLiteral.getValueExpressions().size() != 1) {
              // we only support the case where the spread operation is the only element of the
              // array initializer.
              return newArray;
            }

            Expression uniqueElement = arrayLiteral.getValueExpressions().get(0);
            if (!isRedundantSpreadOperator(uniqueElement)) {
              // The element is not on the form `...new int[] {1,2,3}`
              return newArray;
            }

            return ((PrefixExpression) uniqueElement).getOperand();
          }
        });
  }

  private static boolean isRedundantSpreadOperator(Expression expression) {
    if (!(expression instanceof PrefixExpression)) {
      return false;
    }

    PrefixExpression prefixExpression = (PrefixExpression) expression;

    if (prefixExpression.getOperator() != PrefixOperator.SPREAD) {
      return false;
    }

    if (!(prefixExpression.getOperand() instanceof NewArray)) {
      return false;
    }

    NewArray newArrayExpression = (NewArray) prefixExpression.getOperand();

    if (newArrayExpression.getInitializer() instanceof ArrayLiteral) {
      // Spread operation is redundant on new int[] {1,2,..}
      return true;
    }

    List<Expression> dimensions = newArrayExpression.getDimensionExpressions();

    // Spread operation is redundant on new Foo[0] or new Foo[0][][]
    return newArrayExpression.getInitializer() == null
        && !dimensions.isEmpty()
        && dimensions.get(0) instanceof NumberLiteral
        && ((NumberLiteral) dimensions.get(0)).getValue().intValue() == 0
        && isAllNullLiterals(dimensions.subList(1, dimensions.size()));
  }

  private static boolean isAllNullLiterals(List<Expression> expressions) {
    return expressions.stream().allMatch(e -> e instanceof NullLiteral);
  }
}
