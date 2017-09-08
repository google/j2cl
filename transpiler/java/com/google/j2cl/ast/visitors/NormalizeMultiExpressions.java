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

import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.EmptyStatement;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.Literal;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.UnaryExpression;
import com.google.j2cl.ast.VariableReference;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simplifies multiexpressions to ensure that multiexpressions are never on the lhs of a side
 * effecting operator and that they don't appear directly as statements and that they don't have
 * dead side effect free expressions.
 */
public class NormalizeMultiExpressions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new FlattenMultiExpressions());
    compilationUnit.accept(new SwitchMultiExpressionsAndSideEffectingExpressions());
  }

  private static class FlattenMultiExpressions extends AbstractRewriter {
    @Override
    public Statement rewriteExpressionStatement(ExpressionStatement statement) {
      if (statement.getExpression() instanceof MultiExpression) {
        // Since we're looking at MultiExpressions that are the primary expression of an
        // ExpressionStatement, we know that the return value isn't used.
        // That makes it safe to remove any subexpressions that don't have side effects.
        List<Expression> expressions =
            ((MultiExpression) statement.getExpression())
                .getExpressions()
                .stream()
                .filter(NormalizeMultiExpressions::mayHaveSideEffects)
                .collect(Collectors.toList());

        if (expressions.isEmpty()) {
          // Eliminate empty multi expressions.
          return new EmptyStatement(statement.getSourcePosition());
        } else if (expressions.size() == 1) {
          // If the multi expression contains only one expression, then unwrap it.
          return expressions.get(0).makeStatement(statement.getSourcePosition());
        } else {
          // If there are multiple expressions then turn it into a block so that any var creation
          // appears immediately before its use rather than being hoisted to the top of the
          // enclosing block.
          return new Block(
              statement.getSourcePosition(),
              expressions
                  .stream()
                  .map(
                      innerExpression ->
                          innerExpression.makeStatement(statement.getSourcePosition()))
                  .collect(Collectors.toList()));
        }
      }
      return statement;
    }

    @Override
    public Expression rewriteMultiExpression(MultiExpression multiExpression) {
      List<Expression> flattenedExpressions = new ArrayList<>();
      for (Expression expression : multiExpression.getExpressions()) {
        if (expression instanceof MultiExpression) {
          flattenedExpressions.addAll(((MultiExpression) expression).getExpressions());
        } else {
          flattenedExpressions.add(expression);
        }
      }
      return MultiExpression.newBuilder().setExpressions(flattenedExpressions).build();
    }
  }

  private static class SwitchMultiExpressionsAndSideEffectingExpressions extends AbstractRewriter {
    @Override
    public Expression rewriteBinaryExpression(BinaryExpression expression) {
      if (expression.getOperator().hasSideEffect()
          && expression.getLeftOperand() instanceof MultiExpression) {
        List<Expression> lhsExpressions =
            ((MultiExpression) expression.getLeftOperand()).getExpressions();
        Expression rightMostLhsExpression = Iterables.getLast(lhsExpressions);
        Expression innerExpression =
            BinaryExpression.Builder.from(expression).setLeftOperand(rightMostLhsExpression).build();
        return MultiExpression.newBuilder()
            .addExpressions(lhsExpressions.subList(0, lhsExpressions.size() - 1))
            .addExpressions(innerExpression)
            .build();
      }
      return expression;
    }

    @Override
    public Expression rewriteUnaryExpression(UnaryExpression expression) {
      if (expression.getOperator().hasSideEffect()
          && expression.getOperand() instanceof MultiExpression) {
        List<Expression> expressions = ((MultiExpression) expression.getOperand()).getExpressions();
        Expression rightMostExpression = Iterables.getLast(expressions);
        Expression innerExpression =
            UnaryExpression.Builder.from(expression).setOperand(rightMostExpression).build();
        return MultiExpression.newBuilder()
            .addExpressions(expressions.subList(0, expressions.size() - 1))
            .addExpressions(innerExpression)
            .build();
      }
      return expression;
    }
  }

  private static boolean mayHaveSideEffects(Expression expression) {
    return !(expression instanceof VariableReference || expression instanceof Literal);
  }
}
