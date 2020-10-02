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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.RuntimeMethods;

/** Replaces object == object expressions with Equality.$same(object, object) calls. */
public class NormalizeEquality extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteBinaryExpression(BinaryExpression binaryExpression) {
            // Don't rewrite non-equality expressions.
            if (binaryExpression.getOperator() != BinaryOperator.EQUALS
                && binaryExpression.getOperator() != BinaryOperator.NOT_EQUALS) {
              return binaryExpression;
            }

            // Don't rewrite primitive comparisons since '==' and '!=' are already good enough.
            if (binaryExpression.getLeftOperand().getTypeDescriptor().isPrimitive()
                || binaryExpression.getRightOperand().getTypeDescriptor().isPrimitive()) {
              return binaryExpression;
            }

            // Rewrite object - object comparisons to avoid JS implicit conversions and still treat
            // null and undefined as equivalent.
            MethodCall sameCall =
                RuntimeMethods.createEqualityMethodCall(
                    "$same", binaryExpression.getLeftOperand(), binaryExpression.getRightOperand());
            if (binaryExpression.getOperator() == BinaryOperator.NOT_EQUALS) {
              return sameCall.prefixNot();
            }
            return sameCall;
          }
        });
  }
}
