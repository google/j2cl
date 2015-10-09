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
 * Inserts an unboxing operation when a boxed type is being put into a primitive type slot in
 * casting, assignment, method invocation, unary numeric promotion or binary numeric promotion
 * conversion contexts.
 */
public class InsertUnboxingConversionVisitor extends ConversionContextVisitor {

  public static void applyTo(CompilationUnit compilationUnit) {
    new InsertUnboxingConversionVisitor().run(compilationUnit);
  }

  public InsertUnboxingConversionVisitor() {
    super(
        new ContextRewriter() {

          @Override
          public Expression rewriteAssignmentContext(
              TypeDescriptor toTypeDescriptor, Expression expression) {
            return maybeUnbox(toTypeDescriptor, expression);
          }

          @Override
          public Expression rewriteBinaryNumericPromotionContext(Expression operandExpression) {
            if (TypeDescriptors.isBoxedType(operandExpression.getTypeDescriptor())
                && !TypeDescriptors.isBoxedBooleanOrDouble(operandExpression.getTypeDescriptor())) {
              return AstUtils.unbox(operandExpression);
            }
            return operandExpression;
          }

          @Override
          public Expression rewriteCastContext(CastExpression castExpression) {
            if (TypeDescriptors.isPrimitiveType(castExpression.getCastTypeDescriptor())
                && TypeDescriptors.isBoxedType(castExpression.getExpression().getTypeDescriptor())
                && !TypeDescriptors.isBoxedBooleanOrDouble(
                    castExpression.getExpression().getTypeDescriptor())) {
              // Actually remove the cast and replace it with the unboxing.
              return AstUtils.unbox(castExpression.getExpression());
            }
            return castExpression;
          }

          @Override
          public Expression rewriteMethodInvocationContext(
              TypeDescriptor parameterTypeDescriptor, Expression argumentExpression) {
            return maybeUnbox(parameterTypeDescriptor, argumentExpression);
          }

          @Override
          public Expression rewriteUnaryNumericPromotionContext(Expression operandExpression) {
            if (TypeDescriptors.isBoxedType(operandExpression.getTypeDescriptor())
                && !TypeDescriptors.isBoxedBooleanOrDouble(operandExpression.getTypeDescriptor())) {
              return AstUtils.unbox(operandExpression);
            }
            return operandExpression;
          }
        });
  }

  private static Expression maybeUnbox(TypeDescriptor toTypeDescriptor, Expression expression) {
    if (TypeDescriptors.isPrimitiveType(toTypeDescriptor)
        && TypeDescriptors.isBoxedType(expression.getTypeDescriptor())
        && !TypeDescriptors.isBoxedBooleanOrDouble(expression.getTypeDescriptor())) {
      return AstUtils.unbox(expression);
    }
    return expression;
  }
}
