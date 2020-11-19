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

import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveFloatOrDouble;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveLong;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.Operator;
import com.google.j2cl.transpiler.ast.PrefixOperator;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.UnaryExpression;

/**
 * Rewrites unary expressions that are not supported directly by the wasm backend.
 *
 * <p>Note: assumes that all conversion passes run before this one.
 */
public class RewriteUnaryExpressions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteUnaryExpression(UnaryExpression expression) {
            Operator operator = expression.getOperator();
            Expression operand = expression.getOperand();
            TypeDescriptor operandType = operand.getTypeDescriptor();

            // +a => a. Any necessary conversions (coercions, unboxing, etc) will to be performed by
            // a previous pass.
            if (operator == PrefixOperator.PLUS) {
              return operand;
            }

            // -a => 0 - a; but only for integral types.
            if (operator == PrefixOperator.MINUS && !isPrimitiveFloatOrDouble(operandType)) {
              return createNumberLiteral(0, operandType).infixMinus(operand);
            }

            // ~a => -1 ^ a. (note that -1 is is the value with all bits being ones)
            if (operator == PrefixOperator.COMPLEMENT) {
              return createNumberLiteral(-1, operandType).infixBitwiseXor(operand);
            }

            return expression;
          }

          private Expression createNumberLiteral(int value, TypeDescriptor type) {
            return isPrimitiveLong(type)
                ? NumberLiteral.fromLong(value)
                : NumberLiteral.fromInt(value);
          }
        });
  }
}
