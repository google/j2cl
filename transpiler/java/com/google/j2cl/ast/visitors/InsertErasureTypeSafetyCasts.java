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
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeVariable;

/**
 * Inserts a casts needed for type safety due to type erasure.
 *
 * <p>As part of type erasure casts need to be introduced to preserve Java type safety, as in the
 * following example:
 *
 * <pre>
 *     interface L<T> {
 *       T get();
 *     }
 *
 *     ...
 *
 *     L<String> l = (L<String>) someL;
 *     String s = l.get();
 *  </pre>
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
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteMethodCall(MethodCall methodCall) {
            Expression qualifier = methodCall.getQualifier();
            return MethodCall.Builder.from(methodCall)
                .setQualifier(maybeInsertErasureTypeSafetyCast(qualifier))
                .build();
          }

          @Override
          public Expression rewriteFieldAccess(FieldAccess fieldAccess) {
            Expression qualifier = fieldAccess.getQualifier();
            return FieldAccess.Builder.from(fieldAccess)
                .setQualifier(maybeInsertErasureTypeSafetyCast(qualifier))
                .build();
          }
        });
  }

  private ConversionContextVisitor.ContextRewriter getContextRewriter() {
    return new ConversionContextVisitor.ContextRewriter() {
      @Override
      public Expression rewriteAssignmentContext(
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
      public Expression rewriteMethodInvocationContext(
          ParameterDescriptor toParameterDescriptor,
          ParameterDescriptor declaredParameterDescriptor,
          Expression expression) {
        TypeDescriptor toTypeDescriptor = toParameterDescriptor.getTypeDescriptor();
        TypeDescriptor declaredTypeDescriptor = declaredParameterDescriptor.getTypeDescriptor();
        if (toParameterDescriptor.isVarargs()
            && AstUtils.isNonNativeJsEnumArray(toTypeDescriptor)) {
          // TODO(b/118299062): Remove special casing when non native JsEnum arrays are allowed.
          //
          // Since the packaging of varargs (see AstUtils.getPackagedVarargs() for the motivation)
          // creates an array of type DeclaredType[] instead of a JsEnum[] this pass would normally
          // insert an erasure cast to JsEnum[], which needs to be avoided.
          return expression;
        }
        return rewriteAssignmentContext(toTypeDescriptor, declaredTypeDescriptor, expression);
      }

      @Override
      public Expression rewriteBinaryNumericPromotionContext(
          Expression subjectOperandExpression, Expression otherOperandExpression) {
        return maybeInsertErasureTypeSafetyCast(subjectOperandExpression);
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
    TypeDescriptor leafTypeDescriptor =
        fromTypeDescriptor.isArray()
            ? ((ArrayTypeDescriptor) fromTypeDescriptor).getLeafTypeDescriptor()
            : fromTypeDescriptor;
    if (!(leafTypeDescriptor instanceof TypeVariable)) {
      return expression;
    }

    if (!fromTypeDescriptor.isAssignableTo(toTypeDescriptor)) {
      return CastExpression.newBuilder()
          .setExpression(expression)
          .setCastTypeDescriptor(toTypeDescriptor)
          .build();
    }
    return expression;
  }
}
