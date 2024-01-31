/*
 * Copyright 2023 Google Inc.
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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Type;

/** Rewrites null checks for JsEnum values for Wasm. */
public class RewriteJsEnumNullChecks extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteBinaryExpression(BinaryExpression expression) {
            if (expression.getOperator() != BinaryOperator.EQUALS
                && expression.getOperator() != BinaryOperator.NOT_EQUALS) {
              return expression;
            }

            if (!AstUtils.isPrimitiveNonNativeJsEnum(
                    expression.getLeftOperand().getTypeDescriptor())
                && !AstUtils.isPrimitiveNonNativeJsEnum(
                    expression.getRightOperand().getTypeDescriptor())) {
              return expression;
            }

            if (expression.getOperator() == BinaryOperator.EQUALS) {
              return rewriteJsEnumNullEquality(expression);
            }

            return rewriteJsEnumNullEquality(
                    expression.getLeftOperand().infixEquals(expression.getRightOperand()))
                .prefixNot();
          }
        });
  }

  private static Expression rewriteJsEnumNullEquality(BinaryExpression expression) {
    checkArgument(expression.getOperator() == BinaryOperator.EQUALS);

    if (expression.getRightOperand() instanceof NullLiteral) {
      return RuntimeMethods.createEnumsIsNullCall(expression.getLeftOperand());
    } else if (expression.getLeftOperand() instanceof NullLiteral) {
      return RuntimeMethods.createEnumsIsNullCall(expression.getRightOperand());
    } else {
      return expression;
    }
  }
}
