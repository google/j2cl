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

import com.google.common.base.CaseFormat;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.PrefixExpression;
import com.google.j2cl.transpiler.ast.PrefixOperator;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import javax.annotation.Nullable;

/** Replaces long operations with corresponding long utils method calls. */
public class NormalizeLongs extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteBinaryExpression(BinaryExpression binaryExpression) {
            Expression leftOperand = binaryExpression.getLeftOperand();
            Expression rightOperand = binaryExpression.getRightOperand();
            TypeDescriptor returnTypeDescriptor = binaryExpression.getTypeDescriptor();

            // Skips non-long operations.
            if ((!TypeDescriptors.isPrimitiveLong(leftOperand.getTypeDescriptor())
                    && !TypeDescriptors.isPrimitiveLong(rightOperand.getTypeDescriptor()))
                || (!TypeDescriptors.isPrimitiveLong(returnTypeDescriptor)
                    && !TypeDescriptors.isPrimitiveBoolean(returnTypeDescriptor))) {
              return binaryExpression;
            }
            // Skips assignment because it doesn't need special handling.
            if (binaryExpression.isSimpleAssignment()) {
              return binaryExpression;
            }

            return RuntimeMethods.createLongUtilsMethodCall(
                getLongOperationFunctionName(binaryExpression.getOperator()),
                returnTypeDescriptor,
                leftOperand,
                rightOperand);
          }

          @Override
          public Expression rewritePrefixExpression(PrefixExpression prefixExpression) {
            Expression operand = prefixExpression.getOperand();
            // Only interested in longs.
            if (!TypeDescriptors.isPrimitiveLong(operand.getTypeDescriptor())) {
              return prefixExpression;
            }
            PrefixOperator operator = prefixExpression.getOperator();
            // Unwrap PLUS operator because it's a NOOP.
            if (operator == PrefixOperator.PLUS) {
              return prefixExpression.getOperand();
            }

            // LongUtils.someOperation(operand);
            return RuntimeMethods.createLongUtilsMethodCall(
                getLongOperationFunctionName(operator), operand);
          }
        });
  }

  // TODO(goktug): Remove this method after RewriteUnaryExpressions start running for all backends.
  @Nullable
  private static String getLongOperationFunctionName(PrefixOperator prefixOperator) {
    return switch (prefixOperator) {
      case MINUS -> "negate"; // Multiply by -1;
      case COMPLEMENT -> "not"; // Bitwise not
      default -> throw new IllegalStateException("Unexpected binary operator " + prefixOperator);
    };
  }

  private static String getLongOperationFunctionName(BinaryOperator operator) {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, operator.name());
  }
}
