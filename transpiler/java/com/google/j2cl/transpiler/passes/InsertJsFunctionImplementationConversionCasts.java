/*
 * Copyright 2023 Google Inc.
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

import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.TypeDescriptor;

/**
 * Inserts explicit JsDoc casts when assigning JsFunction implementations to JsFunction interface
 * types and vice-versa.
 *
 * <p>Note: The pass addes the casts in both directions because the constructor of JsFunction
 * implementations was modified to return a JsFunction interface types.
 */
public class InsertJsFunctionImplementationConversionCasts extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ConversionContextVisitor.ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression expression) {
                if (expression instanceof NullLiteral) {
                  // No need to add extra casts to a null literal.
                  return expression;
                }

                if ((inferredTypeDescriptor.isJsFunctionInterface()
                        && expression.getTypeDescriptor().isJsFunctionImplementation())
                    || (inferredTypeDescriptor.isJsFunctionImplementation()
                        && expression.getTypeDescriptor().isJsFunctionInterface())) {
                  return JsDocCastExpression.newBuilder()
                      .setCastTypeDescriptor(inferredTypeDescriptor)
                      .setExpression(expression)
                      .build();
                }
                return expression;
              }
            }));
  }
}
