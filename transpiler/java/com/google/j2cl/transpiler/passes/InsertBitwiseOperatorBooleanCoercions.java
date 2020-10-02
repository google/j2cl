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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/**
 * Normalizes bitwise boolean operations.
 *
 * <p>In Java bitwise operators are defined on boolean operands as well as numeric ones. JavaScript
 * takes a different approach, where bitwise operators are numeric and boolean operands are coerced
 * to numbers returning a numeric value. The Closure type system emits an error when if there are
 * not explicit coercions from number to boolean and vice versa.
 *
 */
public class InsertBitwiseOperatorBooleanCoercions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            if (binaryExpression.getOperator().isBitwiseOperator()
                && TypeDescriptors.isPrimitiveBoolean(binaryExpression.getTypeDescriptor())) {
              checkArgument(!binaryExpression.getOperator().isCompoundAssignment());
              // Perform the following transformation:
              //   boolExp1 ^ boolExp2" -> "!!(+(boolExp1) ^ +(boolExp2))
              return BinaryExpression.Builder.from(binaryExpression)
                  .setLeftOperand(binaryExpression.getLeftOperand().prefixPlus())
                  .setRightOperand(binaryExpression.getRightOperand().prefixPlus())
                  .build()
                  .prefixNot()
                  .prefixNot();
            }
            return binaryExpression;
          }
        });
  }
}
