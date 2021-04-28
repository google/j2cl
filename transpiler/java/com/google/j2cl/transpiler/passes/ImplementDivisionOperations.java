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
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/**
 * Handle division overflow by rewriting division to a method call that implements division safely.
 */
public class ImplementDivisionOperations extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteBinaryExpression(BinaryExpression binaryExpression) {
            TypeDescriptor expressionTypeDescriptor = binaryExpression.getTypeDescriptor();
            if (binaryExpression.getOperator() == BinaryOperator.DIVIDE
                && TypeDescriptors.isIntegralPrimitiveType(expressionTypeDescriptor)) {
              return MethodCall.Builder.from(
                      TypeDescriptors.get()
                          .javaemulInternalPrimitives
                          .getMethodDescriptor(
                              "safeDivision", expressionTypeDescriptor, expressionTypeDescriptor))
                  .setArguments(
                      binaryExpression.getLeftOperand(), binaryExpression.getRightOperand())
                  .build();
            }
            return binaryExpression;
          }
        });
  }
}
