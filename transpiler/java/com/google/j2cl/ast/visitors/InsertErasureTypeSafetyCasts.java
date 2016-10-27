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

import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.TypeDescriptor;

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
  }

  private ConversionContextVisitor.ContextRewriter getContextRewriter() {
    return new ConversionContextVisitor.ContextRewriter() {
      @Override
      public Expression rewriteAssignmentContext(
          TypeDescriptor toTypeDescriptor, Expression expression) {
        return maybeInsertErasureTypeSafetyCast(toTypeDescriptor, expression);
      }

      @Override
      public Expression rewriteMethodInvocationContext(
          TypeDescriptor parameterTypeDescriptor, Expression argumentExpression) {
        return maybeInsertErasureTypeSafetyCast(parameterTypeDescriptor, argumentExpression);
      }
    };
  }

  private static Expression maybeInsertErasureTypeSafetyCast(
      TypeDescriptor toTypeDescriptor, Expression expression) {
    if (expression instanceof MethodCall) {
      MethodDescriptor target = ((MethodCall) expression).getTarget();
      return maybeInsertErasureTypeSafetyCast(
          target.getDeclarationMethodDescriptor().getReturnTypeDescriptor(),
          toTypeDescriptor,
          expression);
    } else if (expression instanceof FieldAccess) {
      FieldDescriptor target = ((FieldAccess) expression).getTarget();
      return maybeInsertErasureTypeSafetyCast(
          target.getDeclarationFieldDescriptor().getTypeDescriptor(), toTypeDescriptor, expression);
    } else {
      return expression;
    }
  }

  private static Expression maybeInsertErasureTypeSafetyCast(
      TypeDescriptor fromTypeDescriptor, TypeDescriptor toTypeDescriptor, Expression expression) {
    if (!fromTypeDescriptor.isTypeVariable() && !fromTypeDescriptor.isWildCardOrCapture()) {
      return expression;
    } else if (!AstUtils.canRemoveCast(fromTypeDescriptor, toTypeDescriptor)) {
      return CastExpression.newBuilder()
          .setExpression(expression)
          .setCastTypeDescriptor(toTypeDescriptor)
          .build();
    }
    return expression;
  }
}
