/*
 * Copyright 2022 Google Inc.
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
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import javax.annotation.Nullable;

/**
 * Statically evaluate String literal comparisons.
 *
 * <p>TODO(b/225081858): Remove this pass after Binaryen optimizes these scenarios.
 */
public class StaticallyEvaluateStringComparison extends NormalizationPass {
  @Override
  public void applyTo(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteBinaryExpression(BinaryExpression expression) {
            if (expression.getOperator() == BinaryOperator.EQUALS) {
              Expression staticResult =
                  tryEvaluateEquality(expression.getLeftOperand(), expression.getRightOperand());
              if (staticResult != null) {
                return staticResult;
              }
            }
            return expression;
          }

          @Override
          public Expression rewriteMethodCall(MethodCall methodCall) {
            if (isStringComparisonMethod(methodCall.getTarget())) {
              Expression staticResult =
                  tryEvaluateEquality(methodCall.getQualifier(), methodCall.getArguments().get(0));
              if (staticResult != null) {
                return staticResult;
              }
            }
            return methodCall;
          }
        });
  }

  @Nullable
  private static Expression tryEvaluateEquality(Expression lhs, Expression rhs) {
    if (lhs instanceof StringLiteral && rhs instanceof StringLiteral) {
      return evaluateEquality((StringLiteral) lhs, (StringLiteral) rhs);
    }
    return null;
  }

  private static Expression evaluateEquality(StringLiteral lhs, StringLiteral rhs) {
    return BooleanLiteral.get(lhs.getValue().equals(rhs.getValue()));
  }

  private static boolean isStringComparisonMethod(MethodDescriptor method) {
    return TypeDescriptors.isJavaLangString(method.getEnclosingTypeDescriptor())
        && method.getName().equals("equals")
        && method.getParameterDescriptors().size() == 1;
  }
}
