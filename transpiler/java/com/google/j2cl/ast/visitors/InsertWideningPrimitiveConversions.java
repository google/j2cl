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
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;

/**
 * Inserts a widening operation when a smaller primitive type is being put into a large primitive
 * type slot in assignment, binary numeric promotion, cast and method invocation conversion
 * contexts.
 *
 * <p>TODO: this pass removes NOOP casts (e.g. int -> float), which may cause wrong side effect if
 * the later passes depend on the cast type. Currently we carefully order these passes in
 * J2clTranspiler to ensure all conversion contexts are correctly caught. But if it turns out the
 * side effect does lead to wrong result, we should remove the NOOP casts in a separate pass.
 */
public class InsertWideningPrimitiveConversions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new ConversionContextVisitor(getContextRewriter()));
  }

  private ConversionContextVisitor.ContextRewriter getContextRewriter() {
    return new ConversionContextVisitor.ContextRewriter() {

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
          Expression subjectOperand, Expression otherOperand) {
        if (!TypeDescriptors.isNumericPrimitive(subjectOperand.getTypeDescriptor())
            || !TypeDescriptors.isNumericPrimitive(otherOperand.getTypeDescriptor())) {
          // Widening only applies between primitive types.
          return subjectOperand;
        }

        TypeDescriptor widenedTypeDescriptor =
            AstUtils.chooseWidenedTypeDescriptor(
                otherOperand.getTypeDescriptor(), subjectOperand.getTypeDescriptor());
        if (!shouldWiden(widenedTypeDescriptor, subjectOperand)) {
          return subjectOperand;
        }
        return widenTo(widenedTypeDescriptor, subjectOperand);
      }

      @Override
      public Expression rewriteCastContext(CastExpression castExpression) {
        if (!shouldWiden(castExpression.getCastTypeDescriptor(), castExpression.getExpression())) {
          return castExpression;
        }
        return widenTo(castExpression.getCastTypeDescriptor(), castExpression.getExpression());
      }

      @Override
      public Expression rewriteMethodInvocationContext(
          ParameterDescriptor parameterDescriptor, Expression argumentExpression) {
        TypeDescriptor parameterTypeDescriptor = parameterDescriptor.getTypeDescriptor();
        if (!shouldWiden(parameterTypeDescriptor, argumentExpression)) {
          return argumentExpression;
        }
        return widenTo(parameterTypeDescriptor, argumentExpression);
      }
    };
  }

  private static boolean shouldWiden(
      TypeDescriptor toTypeDescriptor, Expression subjectExpression) {
    TypeDescriptor fromTypeDescriptor = subjectExpression.getTypeDescriptor();

    if (!TypeDescriptors.isNumericPrimitive(fromTypeDescriptor)
        || !TypeDescriptors.isNumericPrimitive(toTypeDescriptor)) {
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
            AstUtils.toProperCase(fromTypeDescriptor.getSimpleSourceName()),
            AstUtils.toProperCase(toTypeDescriptor.getSimpleSourceName()));
    MethodDescriptor widenMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setEnclosingTypeDescriptor(BootstrapType.PRIMITIVES.getDescriptor())
            .setName(widenMethodName)
            .setParameterTypeDescriptors(fromTypeDescriptor)
            .setReturnTypeDescriptor(toTypeDescriptor)
            .build();
    // Primitives.$widenAToB(expr);
    return MethodCall.Builder.from(widenMethodDescriptor).setArguments(subjectExpression).build();
  }
}
