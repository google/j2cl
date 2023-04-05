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

import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/**
 * Inserts a casts needed for type safety due to type erasure.
 *
 * <p>As part of type erasure casts need to be introduced to preserve Java type safety, as in the
 * following example:
 *
 * <pre>{@code
 * interface L<T> {
 *   T get();
 * }
 *
 * ...
 *
 * L<String> l = (L<String>) someL;
 * String s = l.get();
 *
 * }</pre>
 *
 * <p>The previous code after type erasure looks like
 *
 * <pre>
 *     interface L {
 *       Object get();
 *     }
 *
 *     ...
 *
 *     L l = (L) someL;
 *     String s = (String) l.get();
 *  </pre>
 *
 * <p>Note that without the cast in the last line the code would not have been type safe. The main
 * reason is that the cast {@code (L<String>) } does not check type argument consistency and will
 * succeed even if {@code somel} is {@code (L<Object>) }.
 */
public class InsertErasureTypeSafetyCasts extends NormalizationPass {
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
        // Per JLS 5.2, only insert the cast check if it is required to maintain consistency with
        // the LHS. If the LHS is a primitive type then the proper cast to the boxed type is
        // required.
        return maybeInsertErasureTypeSafetyCast(
            toTypeDescriptor.isPrimitive() ? expression.getTypeDescriptor() : toTypeDescriptor,
            expression);
      }

      @Override
      public Expression rewriteCastContext(CastExpression castExpression) {
        // Explicit casts are treated specifically because ContextRewriter does't treat them as
        // a regular type conversion context however they may hide the ereasure. e.g.
        //
        //   List<Integer> integerList = ....;
        //   int i = (int) integerList.get(0);
        //
        // In this example an erasure cast to Integer needs to be inserted before the unboxing
        // implied by the explict cast to int.
        Expression expression = castExpression.getExpression();
        if (castExpression.getCastTypeDescriptor().isPrimitive()
            && !expression.getTypeDescriptor().isPrimitive()) {
          return CastExpression.Builder.from(castExpression)
              .setExpression(
                  maybeInsertErasureTypeSafetyCast(expression.getTypeDescriptor(), expression))
              .build();
        }
        return castExpression;
      }

      @Override
      public Expression rewriteMethodInvocationContext(
          ParameterDescriptor toParameterDescriptor,
          ParameterDescriptor declaredParameterDescriptor,
          Expression argument) {
        TypeDescriptor toTypeDescriptor = toParameterDescriptor.getTypeDescriptor();
        TypeDescriptor declaredTypeDescriptor = declaredParameterDescriptor.getTypeDescriptor();
        if (toParameterDescriptor.isVarargs()
            && AstUtils.isNonNativeJsEnumArray(toTypeDescriptor)) {
          // TODO(b/118299062): Remove special casing when non native JsEnum arrays are allowed.
          //
          // Since the packaging of varargs (see AstUtils.getPackagedVarargs() for the motivation)
          // creates an array of type DeclaredType[] instead of a JsEnum[] this pass would normally
          // insert an erasure cast to JsEnum[], which needs to be avoided.
          return argument;
        }
        return rewriteTypeConversionContext(toTypeDescriptor, declaredTypeDescriptor, argument);
      }

      @Override
      public Expression rewriteBinaryNumericPromotionContext(
          TypeDescriptor otherOperandTypeDescriptor, Expression operand) {
        return maybeInsertErasureTypeSafetyCast(operand);
      }

      @Override
      public Expression rewriteStringContext(Expression expression) {
        // Erasure casts are only needed in string contexts if the type of the expression is String
        // otherwise it is treated as Object and converted using String.valueOf.
        return TypeDescriptors.isJavaLangString(expression.getTypeDescriptor())
            ? maybeInsertErasureTypeSafetyCast(expression)
            : expression;
      }

      @Override
      public Expression rewriteUnaryNumericPromotionContext(Expression expression) {
        return maybeInsertErasureTypeSafetyCast(expression);
      }

      @Override
      public Expression rewriteBooleanConversionContext(Expression expression) {
        return maybeInsertErasureTypeSafetyCast(expression);
      }

      @Override
      public Expression rewriteSwitchExpressionContext(Expression expression) {
        return maybeInsertErasureTypeSafetyCast(expression);
      }
    };
  }

  private static Expression maybeInsertErasureTypeSafetyCast(Expression expression) {
    return maybeInsertErasureTypeSafetyCast(expression.getTypeDescriptor(), expression);
  }

  private static Expression maybeInsertErasureTypeSafetyCast(
      TypeDescriptor toTypeDescriptor, Expression expression) {
    return maybeInsertErasureTypeSafetyCast(
        expression.getDeclaredTypeDescriptor(), toTypeDescriptor, expression);
  }

  private static Expression maybeInsertErasureTypeSafetyCast(
      TypeDescriptor fromTypeDescriptor, TypeDescriptor toTypeDescriptor, Expression expression) {
    if (!fromTypeDescriptor.isTypeVariable()
        && !fromTypeDescriptor.isIntersection()
        && !(fromTypeDescriptor.isArray()
            && ((ArrayTypeDescriptor) fromTypeDescriptor)
                .getLeafTypeDescriptor()
                .isTypeVariable())) {
      return expression;
    }

    TypeDescriptor inferredTypeDescriptor = expression.getTypeDescriptor();
    // TODO(b/121293394): Use the expression inferred type in more cases, e.g. arrays, since it will
    // provide more accurate type information.
    if (inferredTypeDescriptor instanceof DeclaredTypeDescriptor) {
      // Using the inferred type descriptor, which is either the same or a more precise type,
      // conveys more accurate type information potentially improving the optimizations made by
      // jscompiler. This is a slight, but safe, deviation from Java semantics which requires to
      // insert only the necessary casts to preserve runtime type safety.
      toTypeDescriptor = inferredTypeDescriptor;
    }
    if (!fromTypeDescriptor.toRawTypeDescriptor().isAssignableTo(toTypeDescriptor)) {
      return isUncheckedCast(expression)
          ? JsDocCastExpression.newBuilder()
              .setExpression(expression)
              .setCastType(toTypeDescriptor)
              .build()
          : CastExpression.newBuilder()
              .setExpression(expression)
              .setCastTypeDescriptor(toTypeDescriptor)
              .build();
    }

    return expression;
  }

  private static boolean isUncheckedCast(Expression expression) {
    return expression instanceof MethodCall
        && ((MethodCall) expression).getTarget().isUncheckedCast();
  }
}
