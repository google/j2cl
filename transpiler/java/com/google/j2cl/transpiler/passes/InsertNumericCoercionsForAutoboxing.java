/*
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;

/** Inserts casts where Java would normally autobox but Kotlin does not. */
public final class InsertNumericCoercionsForAutoboxing extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              protected Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression expression) {
                // Java can implicitly narrow numeric literals even when also performing boxing.
                // Kotlin cannot so, we'll just statically convert the literal.
                if (TypeDescriptors.isBoxedType(inferredTypeDescriptor)
                    && expression instanceof NumberLiteral literal) {
                  return new NumberLiteral(
                      inferredTypeDescriptor.toUnboxedType(), literal.getValue());
                }

                // Java allows for autoboxing to a type variable with a boxed type bound; for
                // example, <T extends Long>. In this case T n = 1L; would be permitted in Java,
                // but Kotlin will require an explicit cast.
                if (expression.getTypeDescriptor().isPrimitive()
                    && inferredTypeDescriptor.isTypeVariable()
                    && !inferredTypeDescriptor.isWildcardOrCapture()) {
                  return CastExpression.newBuilder()
                      .setCastTypeDescriptor(inferredTypeDescriptor)
                      .setExpression(expression)
                      .build();
                }
                return expression;
              }
            }));
  }
}
