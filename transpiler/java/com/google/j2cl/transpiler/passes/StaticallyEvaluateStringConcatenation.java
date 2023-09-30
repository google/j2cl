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
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import javax.annotation.Nullable;

/** Performs static evaluation of string concatenation on constants. */
public class StaticallyEvaluateStringConcatenation extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            if (binaryExpression.isStringConcatenation()) {

              StringLiteral lhs = convertToStringLiteral(binaryExpression.getLeftOperand());
              StringLiteral rhs = convertToStringLiteral(binaryExpression.getRightOperand());
              if (lhs != null && rhs != null) {
                return new StringLiteral(lhs.getValue() + rhs.getValue());
              }
            }
            return binaryExpression;
          }
        });
  }

  @Nullable
  private static StringLiteral convertToStringLiteral(Expression expression) {
    if (expression instanceof StringLiteral) {
      return (StringLiteral) expression;
    } else {
      if (expression instanceof NumberLiteral) {
        // Char literals are stored as NumberLiterals with an Integer object as its value. So we
        // need to determine whether it is a char, and if so apply the right conversion to String.
        boolean isChar = TypeDescriptors.isPrimitiveChar(expression.getTypeDescriptor());
        Number value = ((NumberLiteral) expression).getValue();
        return new StringLiteral(
            isChar ? String.valueOf((char) value.intValue()) : String.valueOf(value));
      } else if (expression instanceof BooleanLiteral) {
        return new StringLiteral(String.valueOf(((BooleanLiteral) expression).getValue()));
      }
    }
    return null;
  }
}
