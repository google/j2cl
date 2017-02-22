/*
 * Copyright 2017 Google Inc.
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

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsDocAnnotatedExpression;
import com.google.j2cl.ast.Node;

/** Removes unneeded JsDocAnnotations that arise from transformations. */
public final class RemoveUnneededJsDocAnnotations extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteCastExpression(final CastExpression expression) {
            // There should not be casts at the time this pass runs.
            checkState(false);
            return null;
          }

          @Override
          public Node rewriteJsDocAnnotatedExpression(final JsDocAnnotatedExpression expression) {
            // Nested JsDoc cast annotations don't provide any extra information to JS type
            // checkers. Remove the extras.
            Expression innerExpressionWithoutTypeAnnotation =
                AstUtils.removeTypeAnnotationIfPresent(expression.getExpression());
            if (innerExpressionWithoutTypeAnnotation != expression.getExpression()) {
              return JsDocAnnotatedExpression.Builder.from(expression)
                  .setExpression(innerExpressionWithoutTypeAnnotation)
                  .build();
            }
            return expression;
          }
        });
  }
}
