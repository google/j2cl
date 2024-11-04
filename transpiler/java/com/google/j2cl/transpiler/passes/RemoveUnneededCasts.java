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
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.InstanceOfExpression;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
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
 * if (o instanceof Casts) {
 *   Casts c = (Casts) o;  // remove this cast
 * }
 * </pre>
 */
public class RemoveUnneededCasts extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitIfStatement(IfStatement ifStatement) {
            Expression conditionExpression = ifStatement.getConditionExpression();
            if (!(conditionExpression instanceof InstanceOfExpression)) {
              return;
            }

            InstanceOfExpression instanceOfExpression = (InstanceOfExpression) conditionExpression;
            Expression instanceOfTarget = instanceOfExpression.getExpression();
            if (!hasNoSideEffects(instanceOfTarget)) {
              return;
            }

            Statement thenStatement = ifStatement.getThenStatement();
            CastExpression castExpression = getCast(thenStatement);
            if (castExpression == null) {
              return;
            }

            if (!castMatchesInstanceOf(castExpression, instanceOfExpression)) {
              return;
            }

            // Replace the Java cast by a JsDoc cast to preserve the type of the expression.
            JsDocCastExpression jsDocCast =
                JsDocCastExpression.newBuilder()
                    .setExpression(castExpression.getExpression())
                    .setCastTypeDescriptor(castExpression.getTypeDescriptor())
                    .build();
            replaceIn(thenStatement, castExpression, jsDocCast);
          }
        });
  }

  @Nullable
  private static CastExpression getCast(Statement statement) {
    if (statement instanceof Block) {
      return getCast(Iterables.getFirst(((Block) statement).getStatements(), null));
    }
    if (statement instanceof ExpressionStatement) {
      return getCast((ExpressionStatement) statement);
    }
    return null;
  }

  @Nullable
  private static CastExpression getCast(ExpressionStatement expressionStatement) {
    Expression expression = expressionStatement.getExpression();
    if (expression instanceof VariableDeclarationExpression) {
      VariableDeclarationExpression variableDeclarationExpression =
          (VariableDeclarationExpression) expression;
      return getCast(variableDeclarationExpression);
    }
    return null;
  }

  @Nullable
  private static CastExpression getCast(
      VariableDeclarationExpression variableDeclarationExpression) {
    VariableDeclarationFragment variableDeclarationFragment =
        variableDeclarationExpression.getFragments().get(0);
    Expression variableDeclarationInitializer = variableDeclarationFragment.getInitializer();
    if (variableDeclarationInitializer instanceof CastExpression) {
      return (CastExpression) variableDeclarationInitializer;
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

  private static void replaceIn(Statement statement, Expression oldValue, Expression newValue) {
    statement.accept(
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
