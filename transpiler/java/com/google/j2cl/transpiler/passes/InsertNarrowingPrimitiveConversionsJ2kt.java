/*
 * Copyright 2016 Google Inc.
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
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;

/**
 * Inserts required narrowing primitive conversions for Kotlin.
 *
 * <p>In Kotlin, the conversion between char and integral types must be explicit. This pass will
 * insert explicit casts between char and integral primitive types, as well as other mandatory
 * narrowing casts.
 */
public class InsertNarrowingPrimitiveConversionsJ2kt extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression expression) {
                TypeDescriptor fromTypeDescriptor = expression.getTypeDescriptor();
                TypeDescriptor toTypeDescriptor = declaredTypeDescriptor;

                if (!fromTypeDescriptor.isPrimitive()) {
                  return expression;
                }

                if (!declaredTypeDescriptor.isPrimitive()) {
                  return expression;
                }

                return shouldNarrow(fromTypeDescriptor, toTypeDescriptor)
                    ? CastExpression.newBuilder()
                        .setExpression(expression)
                        .setCastTypeDescriptor(toTypeDescriptor)
                        .build()
                    : expression;
              }
            }));
  }

  private static boolean shouldNarrow(
      TypeDescriptor fromTypeDescriptor, TypeDescriptor toTypeDescriptor) {

    if (!TypeDescriptors.isNumericPrimitive(toTypeDescriptor)) {
      // Non-primitive casts are not narrowing.
      return false;
    }

    if (TypeDescriptors.isPrimitiveChar(fromTypeDescriptor)
        || TypeDescriptors.isPrimitiveChar(toTypeDescriptor)) {
      return true;
    }

    return ((PrimitiveTypeDescriptor) fromTypeDescriptor)
        .isWiderThan((PrimitiveTypeDescriptor) toTypeDescriptor);
  }
}
