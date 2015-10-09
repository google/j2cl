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

import com.google.common.base.Preconditions;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

/**
 * Inserts a boxing operation when a primitive type is being put into a reference type slot in
 * assignment or method invocation conversion contexts.
 */
public class InsertBoxingConversionVisitor extends ConversionContextVisitor {

  public static void applyTo(CompilationUnit compilationUnit) {
    new InsertBoxingConversionVisitor().run(compilationUnit);
  }

  public InsertBoxingConversionVisitor() {
    super(
        new ContextRewriter() {

          @Override
          public Expression rewriteAssignmentContext(
              TypeDescriptor toTypeDescriptor, Expression expression) {
            return maybeBox(toTypeDescriptor, expression);
          }

          @Override
          public Expression rewriteCastContext(CastExpression castExpression) {
            if (!TypeDescriptors.isPrimitiveType(castExpression.getCastTypeDescriptor())
                && TypeDescriptors.isPrimitiveType(
                    castExpression.getExpression().getTypeDescriptor())
                && !TypeDescriptors.isPrimitiveBooleanOrDouble(
                    castExpression.getExpression().getTypeDescriptor())) {
              // Actually remove the cast and replace it with the boxing.
              Expression boxedExpression = AstUtils.box(castExpression.getExpression());
              Preconditions.checkArgument(
                  boxedExpression.getTypeDescriptor() == castExpression.getCastTypeDescriptor());
              return boxedExpression;
            }
            return castExpression;
          }

          @Override
          public Expression rewriteMethodInvocationContext(
              TypeDescriptor parameterTypeDescriptor, Expression argumentExpression) {
            return maybeBox(parameterTypeDescriptor, argumentExpression);
          }
        });
  }

  private static Expression maybeBox(TypeDescriptor toTypeDescriptor, Expression expression) {
    if (!TypeDescriptors.isPrimitiveType(toTypeDescriptor)
        && TypeDescriptors.isPrimitiveType(expression.getTypeDescriptor())
        && !TypeDescriptors.isPrimitiveBooleanOrDouble(expression.getTypeDescriptor())) {
      return AstUtils.box(expression);
    }
    return expression;
  }
}
