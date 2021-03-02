/*
 * Copyright 2017 Google Inc.
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
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrefixExpression;
import com.google.j2cl.transpiler.ast.PrefixOperator;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/**
 * Instruments integer operations to adhere to Java semantics. All integer arithmetic operations
 * need to coerce the result to 32-bit integer values. Additionally division and remainders need to
 * emit ArithmeticExpression if the rhs is 0.
 */
public class InsertIntegerCoercions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            TypeDescriptor expressionTypeDescriptor = binaryExpression.getTypeDescriptor();
            if (!TypeDescriptors.isPrimitiveInt(expressionTypeDescriptor)) {
              // Long operations have already been normalized out, and all other integral operations
              // are exactly of type int.
              return binaryExpression;
            }

            switch (binaryExpression.getOperator()) {
              case DIVIDE:
              case REMAINDER:
                // Division and remainder in addition to coercion to int they also need to
                // throw ArithmeticException if the divisor is 0, and both things are handled by
                // {@link Primitives.coerceDivision}.
                return RuntimeMethods.createPrimitivesMethodCall(
                    "coerceDivision", binaryExpression);
              case TIMES:
                // Multiplication is implemented using Math.imul which will coerce its operands,
                // hence the coercion that might have been inserted by this pass can be removed
                // without changing the semantics.
                return RuntimeMethods.createMathImulMethodCall(
                    removeCoercion(binaryExpression.getLeftOperand()),
                    removeCoercion(binaryExpression.getRightOperand()));
              case RIGHT_SHIFT_UNSIGNED:
                // The unsigned right shift operation (>>>) in JavaScript coerces the result to an
                // unsigned 32-bit integer which is not representable in Java int and needs
                // coercion. However the coercion of its operands can be removed since the
                // JavaScript vm will coerce the operands.
              case PLUS:
              case MINUS:
                // As an optimization all coercions on sequences of additive operations can be
                // removed except for the last enclosing one. That is because JavaScript will
                // preserve integer semantics with 51 bit precision and to achieve an overflow on 51
                // by sequence of additive operations starting with 32-bit ints will require an
                // expression too large that is impractical to write (the compiler will probably
                // fail before compiling an expression that large).
                // For example, a + b + c will become (a + b + c) | 0 instead of
                // ((a + b) | 0) + c) | 0.
                return coerceToInt(
                    BinaryExpression.Builder.from(binaryExpression)
                        .setLeftOperand(removeCoercion(binaryExpression.getLeftOperand()))
                        .setRightOperand(removeCoercion(binaryExpression.getRightOperand()))
                        .build());
              default:
                // Bit-wise and signed shift operations already coerce their parameters to 32-bit
                // ints, as described in ECMA262 section 6.1.6.1.
                return binaryExpression;
            }
          }

          @Override
          public Node rewritePrefixExpression(PrefixExpression prefixExpression) {
            TypeDescriptor expressionTypeDescriptor = prefixExpression.getTypeDescriptor();
            if (prefixExpression.getOperator() == PrefixOperator.MINUS
                && TypeDescriptors.isPrimitiveInt(expressionTypeDescriptor)) {
              // Unary minus is the only unary operation that needs to be handled, since
              // -Integer.MIN_VALUE will overflow and should result in Integer.MIN_VALUE.
              return coerceToInt(prefixExpression);
            }
            return prefixExpression;
          }
        });
  }

  /** Coerces the result of the expression to a 32-bit integer. */
  private static Expression coerceToInt(Expression expression) {
    return expression.infixBitwiseOr(NumberLiteral.fromInt(0));
  }

  /** Removes coercions of integer operations. */
  private static Expression removeCoercion(Expression expression) {
    if (!(expression instanceof BinaryExpression)) {
      return expression;
    }
    BinaryExpression binaryExpression = (BinaryExpression) expression;

    if (isCoercion(binaryExpression)) {
      return binaryExpression.getLeftOperand();
    }
    return expression;
  }

  private static boolean isCoercion(BinaryExpression expression) {
    // An integer operation of the shape  b | 0 is an integer coercion.
    return expression.getOperator() == BinaryOperator.BIT_OR
        && TypeDescriptors.isPrimitiveInt(expression.getTypeDescriptor())
        && isZero(expression.getRightOperand());
  }

  private static boolean isZero(Expression expression) {
    return expression instanceof NumberLiteral
        && ((NumberLiteral) expression).getValue().doubleValue() == 0;
  }
}
