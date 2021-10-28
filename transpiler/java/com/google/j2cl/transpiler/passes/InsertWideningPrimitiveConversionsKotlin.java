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

import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import javax.annotation.Nullable;

/**
 * Inserts a widening operation when a smaller primitive type is being put into a large primitive
 * type slot in assignment, binary numeric promotion, cast and method invocation conversion
 * contexts.
 */
public class InsertWideningPrimitiveConversionsKotlin extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new ConversionContextVisitor(getContextRewriter()));
  }

  private ConversionContextVisitor.ContextRewriter getContextRewriter() {
    return new ConversionContextVisitor.ContextRewriter() {

      @Override
      public Expression rewriteTypeConversionContext(
          TypeDescriptor toTypeDescriptor,
          TypeDescriptor declaredTypeDescriptor,
          Expression expression) {
        Expression widened = widenTo(toTypeDescriptor, expression);
        if (widened == null) {
          return expression;
        }
        return widened;
      }

      @Override
      public Expression rewriteCastContext(CastExpression castExpression) {
        Expression widened =
            widenTo(castExpression.getCastTypeDescriptor(), castExpression.getExpression());
        if (widened == null) {
          return castExpression;
        }
        return widened;
      }
    };
  }

  private static boolean shouldWiden(TypeDescriptor toTypeDescriptor, Expression expression) {
    TypeDescriptor fromTypeDescriptor = expression.getTypeDescriptor();
    if (!TypeDescriptors.isNumericPrimitive(fromTypeDescriptor)
        || !TypeDescriptors.isNumericPrimitive(toTypeDescriptor)) {
      // Widening only applies between primitive types.
      return false;
    }
    return ((PrimitiveTypeDescriptor) toTypeDescriptor)
        .isWiderThan((PrimitiveTypeDescriptor) fromTypeDescriptor);
  }

  @Nullable
  private Expression widenTo(TypeDescriptor toTypeDescriptor, Expression expression) {
    if (!shouldWiden(toTypeDescriptor, expression)) {
      return null;
    }
    // Widen literals at compile time.
    if (expression instanceof NumberLiteral) {
      PrimitiveTypeDescriptor literalTypeDescriptor = (PrimitiveTypeDescriptor) toTypeDescriptor;
      return new NumberLiteral(literalTypeDescriptor, ((NumberLiteral) expression).getValue());
    }
    // Otherwise, insert an explicit cast.
    return CastExpression.newBuilder()
        .setExpression(expression)
        .setCastTypeDescriptor(toTypeDescriptor)
        .build();
  }
}
