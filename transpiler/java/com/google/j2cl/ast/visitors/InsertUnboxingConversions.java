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
package com.google.j2cl.ast.visitors;

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import java.util.Optional;

/**
 * Inserts an unboxing operation (and optionally followed by a widening primitive conversion in some
 * contexts) when a boxed type is being put into a primitive type slot in casting, assignment,
 * method invocation, unary numeric promotion or binary numeric promotion conversion contexts.
 */
public class InsertUnboxingConversions extends NormalizationPass {
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
        Optional<Expression> unboxedExpression = maybeUnboxAndWiden(toTypeDescriptor, expression);
        return unboxedExpression.orElse(expression);
      }

      @Override
      public Expression rewriteBinaryNumericPromotionContext(
          TypeDescriptor otherOperandTypeDescriptor, Expression operand) {
        return maybeUnbox(operand);
      }

      @Override
      public Expression rewriteBooleanConversionContext(Expression operand) {
        return maybeUnbox(operand);
      }

      @Override
      public Expression rewriteCastContext(CastExpression castExpression) {
        Expression expression = castExpression.getExpression();
        TypeDescriptor toTypeDescriptor = castExpression.getCastTypeDescriptor();

        Optional<Expression> unboxedExpression = maybeUnboxAndWiden(toTypeDescriptor, expression);
        return unboxedExpression.orElse(castExpression);
      }

      @Override
      public Expression rewriteUnaryNumericPromotionContext(Expression operand) {
        return maybeUnbox(operand);
      }
    };
  }

  private static Expression maybeUnbox(Expression expression) {
    if (TypeDescriptors.isBoxedType(expression.getTypeDescriptor())) {
      return AstUtils.unbox(expression);
    }
    return expression;
  }

  private static Optional<Expression> maybeUnboxAndWiden(
      TypeDescriptor toTypeDescriptor, Expression expression) {
    TypeDescriptor fromTypeDescriptor = expression.getTypeDescriptor();

    if (TypeDescriptors.isNonVoidPrimitiveType(toTypeDescriptor)
        && TypeDescriptors.isBoxedType(fromTypeDescriptor)) {
      // An unboxing conversion....
      Expression resultExpression = AstUtils.unbox(expression);

      fromTypeDescriptor = resultExpression.getTypeDescriptor();
      checkState(fromTypeDescriptor.isPrimitive() && toTypeDescriptor.isPrimitive());

      // ...optionally followed by a widening primitive conversion.
      if (!fromTypeDescriptor.equals(toTypeDescriptor)) {
        resultExpression =
            CastExpression.newBuilder()
                .setExpression(resultExpression)
                .setCastTypeDescriptor(toTypeDescriptor)
                .build();
      }

      return Optional.of(resultExpression);
    }
    return Optional.empty();
  }
}
