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
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PrefixExpression;
import com.google.j2cl.transpiler.ast.PrefixOperator;

/**
 * Normalize equality for Kotlin, by rewriting:
 *
 * <ul>
 *   <li>{@code "=="} to {@code "==="}
 *   <li>{@code "!="} to {@code "!=="}
 *   <li>{@code "Object.equals(Object)"} to {@code "=="}
 *   <li>{@code "!Object.equals(Object)"} to {@code "!="}
 * </ul>
 */
public class NormalizeEqualityKotlin extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor methodDescriptor = methodCall.getTarget();
            if (!methodDescriptor.isOrOverridesJavaLangObjectMethod()) {
              return methodCall;
            }

            if (!methodDescriptor.getName().equals("equals")) {
              return methodCall;
            }

            return BinaryExpression.newBuilder()
                .setLeftOperand(methodCall.getQualifier())
                .setOperator(BinaryOperator.EQUALS)
                .setRightOperand(methodCall.getArguments().get(0))
                .build();
          }

          @Override
          public Expression rewriteBinaryExpression(BinaryExpression binaryExpression) {
            BinaryOperator sameOperator;
            switch (binaryExpression.getOperator()) {
              case EQUALS:
                sameOperator = BinaryOperator.SAME;
                break;
              case NOT_EQUALS:
                sameOperator = BinaryOperator.NOT_SAME;
                break;
              default:
                return binaryExpression;
            }

            if (binaryExpression.getLeftOperand().getTypeDescriptor().isPrimitive()
                || binaryExpression.getRightOperand().getTypeDescriptor().isPrimitive()) {
              return binaryExpression;
            }

            return BinaryExpression.Builder.from(binaryExpression)
                .setOperator(sameOperator)
                .build();
          }

          @Override
          public Node rewritePrefixExpression(PrefixExpression prefixExpression) {
            if (prefixExpression.getOperator() != PrefixOperator.NOT) {
              return prefixExpression;
            }

            Expression operand = prefixExpression.getOperand();
            if (!(operand instanceof BinaryExpression)) {
              return prefixExpression;
            }

            BinaryExpression binaryOperand = (BinaryExpression) operand;
            if (binaryOperand.getOperator() != BinaryOperator.EQUALS) {
              return prefixExpression;
            }

            return BinaryExpression.newBuilder()
                .setLeftOperand(binaryOperand.getLeftOperand())
                .setOperator(BinaryOperator.NOT_EQUALS)
                .setRightOperand(binaryOperand.getRightOperand())
                .build();
          }
        });
  }
}
