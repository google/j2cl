/*
 * Copyright 2023 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.Node;

/**
 * Kotlin compiler transforms Boolean shortcut operations to conditional expressions: {@code a && b}
 * becomes {@code a ? b : false} and {@code a || b} becomes {@code a ? true : b}
 *
 * <p>This pass detects these conditional forms and rewrites them back to the original boolean
 * binary expressions.
 */
public class RecoverShortcutBooleanOperator extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteConditionalExpression(ConditionalExpression conditionalExpression) {
            if (conditionalExpression.getTrueExpression() == BooleanLiteral.get(true)) {
              return BinaryExpression.newBuilder()
                  .setOperator(BinaryOperator.CONDITIONAL_OR)
                  .setLeftOperand(conditionalExpression.getConditionExpression())
                  .setRightOperand(conditionalExpression.getFalseExpression())
                  .build();
            }

            if (conditionalExpression.getFalseExpression() == BooleanLiteral.get(false)) {
              return BinaryExpression.newBuilder()
                  .setOperator(BinaryOperator.CONDITIONAL_AND)
                  .setLeftOperand(conditionalExpression.getConditionExpression())
                  .setRightOperand(conditionalExpression.getTrueExpression())
                  .build();
            }

            return conditionalExpression;
          }
        });
  }
}
