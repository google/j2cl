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

import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

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
      public Expression rewriteAssignmentContext(
          TypeDescriptor toTypeDescriptor, Expression expression) {
        return maybeUnboxAndWiden(toTypeDescriptor, expression);
      }

      @Override
      public Expression rewriteBinaryNumericPromotionContext(
          Expression subjectOperandExpression, Expression otherOperandExpression) {
        if (TypeDescriptors.isBoxedType(subjectOperandExpression.getTypeDescriptor())) {
          return AstUtils.unbox(subjectOperandExpression);
        }
        return subjectOperandExpression;
      }

      @Override
      public Expression rewriteCastContext(CastExpression castExpression) {
        TypeDescriptor fromTypeDescriptor = castExpression.getExpression().getTypeDescriptor();
        TypeDescriptor toTypeDescriptor = castExpression.getCastTypeDescriptor();
        if (TypeDescriptors.isNonVoidPrimitiveType(toTypeDescriptor)
            && TypeDescriptors.isBoxedType(fromTypeDescriptor)) {

          // An unboxing conversion....
          Expression resultExpression = AstUtils.unbox(castExpression.getExpression());

          // ...optionally followed by a widening primitive conversion.
          fromTypeDescriptor = resultExpression.getTypeDescriptor();
          if (!fromTypeDescriptor.hasSameRawType(toTypeDescriptor)) {
            resultExpression =
                CastExpression.newBuilder()
                    .setExpression(resultExpression)
                    .setCastTypeDescriptor(toTypeDescriptor)
                    .build();
          }

          return resultExpression;
        }
        return castExpression;
      }

      @Override
      public Expression rewriteMethodInvocationContext(
          TypeDescriptor parameterTypeDescriptor, Expression argumentExpression) {
        return maybeUnboxAndWiden(parameterTypeDescriptor, argumentExpression);
      }

      @Override
      public Expression rewriteUnaryNumericPromotionContext(Expression operandExpression) {
        if (TypeDescriptors.isBoxedType(operandExpression.getTypeDescriptor())) {
          return AstUtils.unbox(operandExpression);
        }
        return operandExpression;
      }
    };
  }

  private static Expression maybeUnboxAndWiden(
      TypeDescriptor toTypeDescriptor, Expression expression) {
    TypeDescriptor fromTypeDescriptor = expression.getTypeDescriptor();

    if (TypeDescriptors.isNonVoidPrimitiveType(toTypeDescriptor)
        && TypeDescriptors.isBoxedType(fromTypeDescriptor)) {
      // An unboxing conversion....
      Expression resultExpression = AstUtils.unbox(expression);

      // ...optionally followed by a widening primitive conversion.
      fromTypeDescriptor = resultExpression.getTypeDescriptor();
      if (!fromTypeDescriptor.hasSameRawType(toTypeDescriptor)) {
        resultExpression =
            CastExpression.newBuilder()
                .setExpression(resultExpression)
                .setCastTypeDescriptor(toTypeDescriptor)
                .build();
      }

      return resultExpression;
    }
    return expression;
  }
}
