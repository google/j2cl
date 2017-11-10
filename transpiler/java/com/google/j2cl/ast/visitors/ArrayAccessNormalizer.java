/*
 * Copyright 2017 Google Inc.
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
package com.google.j2cl.ast.visitors;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.RuntimeMethods;

/**
 * Rewrites array set operations to use Arrays.$set or LongUtils.$set operations.
 */
public class ArrayAccessNormalizer extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteBinaryExpression(BinaryExpression expression) {
            if (expression.getOperator().hasSideEffect()
                && expression.getLeftOperand() instanceof ArrayAccess) {
              checkArgument(expression.getOperator() == BinaryOperator.ASSIGN);
              ArrayAccess leftSide = (ArrayAccess) expression.getLeftOperand();
              // Return a call to an Arrays or LongUtils array assignment method.
              return RuntimeMethods.createArraySetMethodCall(
                  leftSide.getArrayExpression(),
                  leftSide.getIndexExpression(),
                  expression.getRightOperand());
            }
            return expression;
          }
        });
  }
}
