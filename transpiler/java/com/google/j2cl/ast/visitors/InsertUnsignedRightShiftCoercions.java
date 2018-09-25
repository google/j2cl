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
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;

/**
 * Coerces the result of an unsigned shift due to different semantics in Java and JavaScript.
 *
 * <p>In Java {@code x >>> 0 == x} but in JavaScript the result is the positive unsigned integer,
 * e.g. -1 >>> 0 = 4294967295 instead of -1.
 */
public class InsertUnsignedRightShiftCoercions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            // Maybe perform this transformation:
            // "x >>> y" -> "(x >>> y | 0)"
            if (binaryExpression.getOperator() == BinaryOperator.RIGHT_SHIFT_UNSIGNED) {
              return BinaryExpression.newBuilder()
                  .setLeftOperand(binaryExpression)
                  .setOperator(BinaryOperator.BIT_OR)
                  .setRightOperand(NumberLiteral.fromInt(0))
                  .build()
                  .parenthesize();
            }
            return binaryExpression;
          }
        });
  }
}
