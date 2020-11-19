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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;

/** Rewrites logical shortcutting operators as ternaries. */
public class RewriteShortcutOperators extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            switch (binaryExpression.getOperator()) {
              case CONDITIONAL_OR:
                // a || b => a ? true : b
                return ConditionalExpression.newBuilder()
                    .setConditionExpression(binaryExpression.getLeftOperand())
                    .setTrueExpression(BooleanLiteral.get(true))
                    .setFalseExpression(binaryExpression.getRightOperand())
                    .setTypeDescriptor(PrimitiveTypes.BOOLEAN)
                    .build();
              case CONDITIONAL_AND:
                // a && b => a ? b : true
                return ConditionalExpression.newBuilder()
                    .setConditionExpression(binaryExpression.getLeftOperand())
                    .setTrueExpression(binaryExpression.getRightOperand())
                    .setFalseExpression(BooleanLiteral.get(false))
                    .setTypeDescriptor(PrimitiveTypes.BOOLEAN)
                    .build();
              default:
                return binaryExpression;
            }
          }
        });
  }
}
