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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.StringLiteral;

/** Rewrite String concatenation to {@code String.concat}. */
public class ImplementStringConcatenation extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            if (!binaryExpression.isStringConcatenation()) {
              return binaryExpression;
            }

            Expression left = binaryExpression.getLeftOperand();
            Expression right = binaryExpression.getRightOperand();

            // Skip empty string on concat; esp. happens with common patterns like ("" + x).
            if (isEmptyString(left)) {
              return right;
            }
            if (isEmptyString(right)) {
              return left;
            }

            return RuntimeMethods.createStringConcatMethodCall(left, right);
          }
        });
  }

  private static boolean isEmptyString(Expression expression) {
    return expression instanceof StringLiteral && ((StringLiteral) expression).getValue().isEmpty();
  }
}
