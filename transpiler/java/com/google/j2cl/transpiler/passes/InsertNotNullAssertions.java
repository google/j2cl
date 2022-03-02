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

import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.PostfixExpression;
import com.google.j2cl.transpiler.ast.PostfixOperator;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;

/**
 * Inserts NOT_NULL_ASSERTION (!!) in Kotlin for nullable expressions in places where Kotlin
 * requires a non-null value.
 */
public final class InsertNotNullAssertions extends NormalizationPass {
  @Override
  public void applyTo(Type type) {
    type.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor actualTypeDescriptor,
                  Expression expression) {
                return !inferredTypeDescriptor.isNullable()
                    ? insertNotNullAssertionIfNeeded(expression)
                    : expression;
              }
            }));
  }

  private static Expression insertNotNullAssertionIfNeeded(Expression expression) {
    return expression.getTypeDescriptor().isNullable()
        ? PostfixExpression.newBuilder()
            .setOperand(expression)
            .setOperator(PostfixOperator.NOT_NULL_ASSERTION)
            .build()
        : expression;
  }
}
