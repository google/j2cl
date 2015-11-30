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

import com.google.common.collect.Lists;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptorBuilder;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;

/**
 * Inserts a widening operation when a smaller primitive type is being put into a large primitive
 * type slot in assignment, binary numeric promotion, cast and method invocation conversion
 * contexts.
 */
public class InsertWideningPrimitiveConversionVisitor extends ConversionContextVisitor {

  public static void applyTo(CompilationUnit compilationUnit) {
    new InsertWideningPrimitiveConversionVisitor().run(compilationUnit);
  }

  public InsertWideningPrimitiveConversionVisitor() {
    super(
        new ContextRewriter() {

          @Override
          public Expression rewriteAssignmentContext(
              TypeDescriptor toTypeDescriptor, Expression expression) {
            if (!shouldWiden(toTypeDescriptor, expression)) {
              return expression;
            }
            return widenTo(toTypeDescriptor, expression);
          }

          @Override
          public Expression rewriteBinaryNumericPromotionContext(
              Expression subjectOperandExpression, Expression otherOperandExpression) {
            if (!otherOperandExpression.getTypeDescriptor().isPrimitive()
                || !subjectOperandExpression.getTypeDescriptor().isPrimitive()) {
              // Widening only applies between primitive types.
              return subjectOperandExpression;
            }

            TypeDescriptor widenedTypeDescriptor =
                AstUtils.chooseWidenedTypeDescriptor(
                    otherOperandExpression.getTypeDescriptor(),
                    subjectOperandExpression.getTypeDescriptor());
            if (!shouldWiden(widenedTypeDescriptor, subjectOperandExpression)) {
              return subjectOperandExpression;
            }
            return widenTo(widenedTypeDescriptor, subjectOperandExpression);
          }

          @Override
          public Expression rewriteCastContext(CastExpression castExpression) {
            if (!shouldWiden(
                castExpression.getCastTypeDescriptor(), castExpression.getExpression())) {
              return castExpression;
            }
            return widenTo(castExpression.getCastTypeDescriptor(), castExpression.getExpression());
          }

          @Override
          public Expression rewriteMethodInvocationContext(
              TypeDescriptor parameterTypeDescriptor, Expression argumentExpression) {
            if (!shouldWiden(parameterTypeDescriptor, argumentExpression)) {
              return argumentExpression;
            }
            return widenTo(parameterTypeDescriptor, argumentExpression);
          }
        });
  }

  private static boolean shouldWiden(
      TypeDescriptor toTypeDescriptor, Expression subjectExpression) {
    TypeDescriptor fromTypeDescriptor = subjectExpression.getTypeDescriptor();

    if (!fromTypeDescriptor.isPrimitive() || !toTypeDescriptor.isPrimitive()) {
      // Widening only applies between primitive types.
      return false;
    }

    int fromWidth = TypeDescriptors.getWidth(fromTypeDescriptor);
    int toWidth = TypeDescriptors.getWidth(toTypeDescriptor);

    // Don't modify non-widening casts.
    if (fromWidth >= toWidth) {
      return false;
    }

    return true;
  }

  private static Expression widenTo(TypeDescriptor toTypeDescriptor, Expression subjectExpression) {
    TypeDescriptor fromTypeDescriptor = subjectExpression.getTypeDescriptor();
    // Don't emit known NOOP widenings.
    if (AstUtils.canRemoveCast(fromTypeDescriptor, toTypeDescriptor)) {
      return subjectExpression;
    }

    String widenMethodName =
        String.format(
            "$widen%sTo%s",
            AstUtils.toProperCase(fromTypeDescriptor.getSimpleName()),
            AstUtils.toProperCase(toTypeDescriptor.getSimpleName()));
    MethodDescriptor widenMethodDescriptor =
        MethodDescriptorBuilder.fromDefault()
            .isRaw(true)
            .isStatic(true)
            .enclosingClassTypeDescriptor(BootstrapType.PRIMITIVES.getDescriptor())
            .methodName(widenMethodName)
            .parameterTypeDescriptors(Lists.newArrayList(fromTypeDescriptor))
            .returnTypeDescriptor(toTypeDescriptor)
            .build();
    // Primitives.$widenAToB(expr);
    return MethodCall.createRegularMethodCall(null, widenMethodDescriptor, subjectExpression);
  }
}
