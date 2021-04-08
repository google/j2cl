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

import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveFloat;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveFloatOrDouble;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/** Rewrites remainder operation on floating point as call to helper methods. */
public class ImplementFloatingPointRemainderOperation extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            if (binaryExpression.getOperator() != BinaryOperator.REMAINDER
                || !isPrimitiveFloatOrDouble(
                    binaryExpression.getLeftOperand().getTypeDescriptor())) {
              return binaryExpression;
            }

            String modMethodName =
                isPrimitiveFloat(binaryExpression.getLeftOperand().getTypeDescriptor())
                    ? "fmod"
                    : "dmod";

            return MethodCall.Builder.from(
                    TypeDescriptors.get()
                        .javaemulInternalPrimitives
                        .getMethodDescriptorByName(modMethodName))
                .setArguments(binaryExpression.getLeftOperand(), binaryExpression.getRightOperand())
                .build();
          }
        });
  }
}
