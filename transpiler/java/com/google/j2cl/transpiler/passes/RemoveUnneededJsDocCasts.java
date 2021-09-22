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
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/** Removes unneeded JsDocAnnotations that arise from transformations. */
public final class RemoveUnneededJsDocCasts extends NormalizationPass {
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
          public ExpressionStatement rewriteExpressionStatement(
              ExpressionStatement expressionStatement) {
            Expression expression = expressionStatement.getExpression();
            if (expression instanceof JsDocCastExpression) {
              // Since the result is not used, the JsDoc cast is irrelevant and can be removed.
              return ((JsDocCastExpression) expression)
                  .getExpression()
                  .makeStatement(expressionStatement.getSourcePosition());
            }
            return expressionStatement;
          }

          @Override
          public Expression rewriteMultiExpression(MultiExpression multiExpression) {
            // Remove JsDoc casts from all the expressions. Except for the last expression, the
            // result of each expression is not used, so any JsDoc cast on those is not relevant.
            Expression multiExpressionWithoutJsDocCasts =
                MultiExpression.newBuilder()
                    .setExpressions(
                        multiExpression.getExpressions().stream()
                            .map(AstUtils::removeJsDocCastIfPresent)
                            .collect(toImmutableList()))
                    .build();

            // The only JsDoc cast that is relevant is the one on the last expression and that can
            // be moved outwards.
            Expression lastExpression = Iterables.getLast(multiExpression.getExpressions());
            return lastExpression instanceof JsDocCastExpression
                // Move the JsDoc cast outwards if any.
                ? JsDocCastExpression.Builder.from((JsDocCastExpression) lastExpression)
                    .setExpression(multiExpressionWithoutJsDocCasts)
                    .build()
                : multiExpressionWithoutJsDocCasts;
          }

          @Override
          public Node rewriteJsDocCastExpression(JsDocCastExpression expression) {
            if (TypeDescriptors.isJavaLangObject(expression.getTypeDescriptor())) {
              // Casting to any type doesn't have value so remove the cast.
              return expression.getExpression();
            }
            // Nested JsDoc cast annotations don't provide any extra information to JS type
            // checkers. Remove the extras.
            Expression innerExpressionWithoutTypeAnnotation =
                AstUtils.removeJsDocCastIfPresent(expression.getExpression());
            if (innerExpressionWithoutTypeAnnotation != expression.getExpression()) {
              return JsDocCastExpression.Builder.from(expression)
                  .setExpression(innerExpressionWithoutTypeAnnotation)
                  .build();
            }
            return expression;
          }
        });
  }
}
