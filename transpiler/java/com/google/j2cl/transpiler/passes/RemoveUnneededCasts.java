/*
 * Copyright 2021 Google Inc.
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

import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.InstanceOfExpression;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SuperReference;
import com.google.j2cl.transpiler.ast.ThisOrSuperReference;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import com.google.j2cl.transpiler.ast.VariableReference;
import javax.annotation.Nullable;

/**
 * Replace redundant casts in situations where we are sure that an object successfully casts to a
 * particular class using {@code instanceof} with JSDocCastExpressions (pseudocasts). We can get rid
 * of these casts because of JavaScript's loose Typing rules. For example;
 *
 * <pre>
 * if (o instanceof A) {
 *   A a = (A) o;  // (A) can be removed here.
 * }
 * </pre>
 *
 * or in
 *
 * <pre>
 * o instanceof A ? (A) o : e;  // (A) can be removed here.
 * </pre>
 */
public class RemoveUnneededCasts extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Statement rewriteIfStatement(IfStatement ifStatement) {
            Statement optimizedThenStatement =
                optimizeCast(ifStatement.getConditionExpression(), ifStatement.getThenStatement());
            if (optimizedThenStatement == null) {
              // The statement was not optimized.
              return ifStatement;
            }
            return IfStatement.Builder.from(ifStatement)
                .setThenStatement(optimizedThenStatement)
                .build();
          }

          @Override
          public Expression rewriteConditionalExpression(
              ConditionalExpression conditionalExpression) {
            Expression optimizedTrueExpression =
                optimizeCast(
                    conditionalExpression.getConditionExpression(),
                    conditionalExpression.getTrueExpression());
            if (optimizedTrueExpression == null) {
              // The expression was not optimized.
              return conditionalExpression;
            }
            return ConditionalExpression.Builder.from(conditionalExpression)
                .setTrueExpression(optimizedTrueExpression)
                .build();
          }
        });
  }

  /**
   * Returns an optimized {@code inNode} if the condition {@code conditionExpression} allows to
   * remove {@code castToOptimize}.
   */
  private static <T extends Node> T optimizeCast(Expression conditionExpression, T inNode) {
    if (!(conditionExpression instanceof InstanceOfExpression)) {
      return null;
    }

    InstanceOfExpression instanceOfExpression = (InstanceOfExpression) conditionExpression;
    Expression instanceOfTarget = instanceOfExpression.getExpression();
    if (!hasNoSideEffects(instanceOfTarget)) {
      return null;
    }

    CastExpression castExpression = getCast(inNode);
    if (castExpression == null) {
      return null;
    }

    if (!castMatchesInstanceOf(castExpression, instanceOfExpression)) {
      return null;
    }

    // Replace the Java cast by a JsDoc cast to preserve the type of the expression.
    JsDocCastExpression jsDocCast =
        JsDocCastExpression.newBuilder()
            .setExpression(castExpression.getExpression())
            .setCastTypeDescriptor(castExpression.getTypeDescriptor())
            .build();

    return replaceIn(inNode, castExpression, jsDocCast);
  }

  /** Returns a cast expression on if it is the first evaluable node in {@code Node}. */
  @Nullable
  private static CastExpression getCast(Node node) {
    if (node instanceof Block) {
      return getCast(Iterables.getFirst(((Block) node).getStatements(), null));
    }
    if (node instanceof ExpressionStatement) {
      return getCast(((ExpressionStatement) node).getExpression());
    }
    if (node instanceof CastExpression) {
      return (CastExpression) node;
    }
    if (node instanceof VariableDeclarationExpression) {
      VariableDeclarationFragment variableDeclarationFragment =
          ((VariableDeclarationExpression) node).getFragments().get(0);
      Expression variableDeclarationInitializer = variableDeclarationFragment.getInitializer();
      return getCast(variableDeclarationInitializer);
    }
    return null;
  }

  private static boolean castMatchesInstanceOf(
      CastExpression castExpression, InstanceOfExpression instanceOfExpression) {
    return castExpression
            .getCastTypeDescriptor()
            .equals(instanceOfExpression.getTestTypeDescriptor())
        && isEquivalentCastTarget(
            castExpression.getExpression(), instanceOfExpression.getExpression());
  }

  /**
   * Two expressions are considered equivalent Cast targets if they refer to the same field or
   * variable and are of the types {@code VariableReference} or {@code FieldAccess}.
   *
   * <p>Note: The correctness of this function relies on all the nodes between and including the
   * InstanceOfExpression and the CastExpression to have no side-effects. This condition is already
   * enforced earlier in the pass.
   */
  private static boolean isEquivalentCastTarget(Expression e1, Expression e2) {
    if (e1 instanceof FieldAccess && e2 instanceof FieldAccess) {
      FieldAccess fa1 = (FieldAccess) e1;
      FieldAccess fa2 = (FieldAccess) e2;
      return fa1.getTarget()
              .getDeclarationDescriptor()
              .equals(fa2.getTarget().getDeclarationDescriptor())
          && isEquivalentCastTarget(fa1.getQualifier(), fa2.getQualifier());
    }
    if (e1 instanceof VariableReference && e2 instanceof VariableReference) {
      VariableReference vr1 = (VariableReference) e1;
      VariableReference vr2 = (VariableReference) e2;
      return vr1.getTarget().equals(vr2.getTarget());
    }

    return (e1 instanceof ThisReference && e2 instanceof ThisReference)
        || (e1 instanceof SuperReference && e2 instanceof SuperReference);
  }

  // Expression.hasSideEffects is too conservative. It disregards all FieldAccess Objects.
  private static boolean hasNoSideEffects(Expression expression) {
    if (expression instanceof FieldAccess) {
      FieldAccess fieldAccess = (FieldAccess) expression;
      boolean fieldHasNoSideEffect = !fieldAccess.getTarget().isStatic();
      return fieldHasNoSideEffect || hasNoSideEffects(fieldAccess.getQualifier());
    }
    return expression instanceof ThisOrSuperReference || expression instanceof VariableReference;
  }

  // Note: This is not optimal since it continues iterating through all subnodes without stopping
  // after replacement.

  @SuppressWarnings("unchecked")
  private static <T extends Node> T replaceIn(T node, Expression oldValue, Expression newValue) {
    return (T)
        node.rewrite(
            new AbstractRewriter() {
              @Override
              public Expression rewriteExpression(Expression expr) {
                if (expr == oldValue) {
                  return newValue;
                }
                return expr;
              }
            });
  }
}
