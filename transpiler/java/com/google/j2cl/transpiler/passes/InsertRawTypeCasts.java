/*
 * Copyright 2022 Google Inc.
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

import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.SuperReference;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;

/** Inserts explicit cast between RAW and non-RAW types. */
public final class InsertRawTypeCasts extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor actualTypeDescriptor,
                  Expression expression) {
                // "super" is not an expression in Kotlin (nor in Java). It can only be used
                // directly as a qualifier, hence it can not be cast.
                if (expression instanceof SuperReference) {
                  return expression;
                }

                return expression.getTypeDescriptor().isRaw() != inferredTypeDescriptor.isRaw()
                    ? convertToCastExpression(expression, inferredTypeDescriptor)
                    : expression;
              }
            }));
  }

  private static Expression convertToCastExpression(
      Expression expression, TypeDescriptor castTypeDescriptor) {
    // Re-use existing cast expression if possible.
    CastExpression.Builder castExpressionBuilder =
        expression instanceof CastExpression
            ? CastExpression.Builder.from((CastExpression) expression)
            : CastExpression.newBuilder().setExpression(expression);
    return castExpressionBuilder.setCastTypeDescriptor(castTypeDescriptor).build();
  }
}
