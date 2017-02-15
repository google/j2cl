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
package com.google.j2cl.ast.visitors;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;

/**
 * Instruments integer division and remainders to emit ArithmeticExpression if necessary and coerce
 * the result.
 */
public class InsertDivisionCoercions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    final MethodDescriptor coerceDivisionMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setEnclosingClassTypeDescriptor(BootstrapType.PRIMITIVES.getDescriptor())
            .setName("$coerceDivision")
            .setParameterTypeDescriptors(TypeDescriptors.get().primitiveInt)
            .setReturnTypeDescriptor(TypeDescriptors.get().primitiveInt)
            .build();

    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            boolean isDivision =
                binaryExpression.getOperator() == BinaryOperator.DIVIDE
                    || binaryExpression.getOperator() == BinaryOperator.REMAINDER;
            TypeDescriptor expressionTypeDescriptor = binaryExpression.getTypeDescriptor();
            if (isDivision
                && TypeDescriptors.isNumericPrimitive(expressionTypeDescriptor)
                && !TypeDescriptors.isPrimitiveFloatOrDouble(expressionTypeDescriptor)) {
              // Long operations have already been normalized out.
              checkArgument(!TypeDescriptors.isPrimitiveLong(expressionTypeDescriptor));

              return MethodCall.Builder.from(coerceDivisionMethodDescriptor)
                  .setArguments(binaryExpression)
                  .build();
            }
            return binaryExpression;
          }
        });
  }
}
