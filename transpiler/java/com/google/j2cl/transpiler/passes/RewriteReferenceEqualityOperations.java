/*
 * Copyright 2020 Google Inc.
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
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/**
 * Rewrites equality operations on references.
 *
 * <p>Rewrites {@code a != b} if it is a comparison by reference to {@code !(a == b)}.
 *
 * <p>Rewrites {@code a == null} or {@code null == a} to {@code Platforms.isNull(a)}.
 */
public class RewriteReferenceEqualityOperations extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression expression) {
            if (!expression.isReferenceComparison()) {
              return expression;
            }

            if (expression.getOperator() == BinaryOperator.EQUALS) {
              return rewriteNullEquality(expression);
            } else {
              return rewriteNullEquality(
                      expression.getLeftOperand().infixEquals(expression.getRightOperand()))
                  .prefixNot();
            }
          }
        });
  }

  private static Expression rewriteNullEquality(BinaryExpression expression) {
    checkArgument(expression.getOperator() == BinaryOperator.EQUALS);

    if (expression.getRightOperand() instanceof NullLiteral) {
      return createIsNullCall(expression.getLeftOperand());
    } else if (expression.getLeftOperand() instanceof NullLiteral) {
      return createIsNullCall(expression.getRightOperand());
    } else {
      return expression;
    }
  }

  private static Expression createIsNullCall(Expression reference) {
    return MethodCall.Builder.from(
            TypeDescriptors.get().javaemulInternalPlatform.getMethodDescriptorByName("isNull"))
        .setArguments(reference)
        .build();
  }
}
