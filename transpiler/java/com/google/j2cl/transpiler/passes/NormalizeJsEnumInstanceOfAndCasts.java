/*
 * Copyright 2023 Google Inc.
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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.InstanceOfExpression;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;

/**
 * Replaces instanceof expression with corresponding $isInstance method call for JsEnums, and
 * removes unnecessary casts.
 */
public class NormalizeJsEnumInstanceOfAndCasts extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteInstanceOfExpression(InstanceOfExpression expression) {
            if (!AstUtils.isNonNativeJsEnum(expression.getTestTypeDescriptor())) {
              return expression;
            }

            // Replace trivial instanceof expression with a null check.
            Expression subject = expression.getExpression();
            if (subject.getTypeDescriptor().isAssignableTo(expression.getTestTypeDescriptor())) {
              return RuntimeMethods.createEnumsBoxMethodCall(subject).infixNotEqualsNull();
            }

            return RuntimeMethods.createEnumsInstanceOfMethodCall(
                subject, expression.getTestTypeDescriptor());
          }

          @Override
          public Expression rewriteJsDocCastExpression(JsDocCastExpression expression) {
            if (shouldRemoveCast(expression.getTypeDescriptor())) {
              return expression.getExpression();
            }
            return expression;
          }

          @Override
          public Expression rewriteCastExpression(CastExpression expression) {
            if (shouldRemoveCast(expression.getCastTypeDescriptor())) {
              return expression.getExpression();
            }
            return expression;
          }
        });
  }

  private static boolean shouldRemoveCast(TypeDescriptor toTypeDescriptor) {
    return toTypeDescriptor.isPrimitive() || AstUtils.isNonNativeJsEnum(toTypeDescriptor);
  }
}
