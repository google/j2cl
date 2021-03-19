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
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.TypeDescriptor;

/** Normalize shift arithmetic operations to have correct type of operands. */
public class NormalizeShifts extends NormalizationPass {

  private final boolean narrowAllToInt;

  public NormalizeShifts() {
    this(true);
  }

  public NormalizeShifts(boolean narrowAllToInt) {
    this.narrowAllToInt = narrowAllToInt;
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteBinaryExpression(BinaryExpression binaryExpression) {
            if (!binaryExpression.getOperator().isShiftOperator()) {
              return binaryExpression;
            }

            // Closure expects 'int' for shift operations while Wasm expects type of lhs.
            // We achieve necessary widening/narrowing operations by adding implicit casts here.
            Expression rightOperand =
                castTo(
                    binaryExpression.getRightOperand(),
                    narrowAllToInt
                        ? PrimitiveTypes.INT
                        : binaryExpression.getLeftOperand().getTypeDescriptor().toUnboxedType());

            return BinaryExpression.Builder.from(binaryExpression)
                .setRightOperand(rightOperand)
                .build();
          }
        });
  }

  private static Expression castTo(Expression expression, TypeDescriptor toTypeDescriptor) {
    return CastExpression.newBuilder()
        .setExpression(expression)
        .setCastTypeDescriptor(toTypeDescriptor)
        .build();
  }
}
