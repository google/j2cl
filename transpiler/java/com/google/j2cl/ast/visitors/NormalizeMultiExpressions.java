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
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.EmptyStatement;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.ExpressionWithComment;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.Invocation;
import com.google.j2cl.ast.Literal;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.ThisReference;
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
    public boolean shouldProcessMethod(Method method) {
      // Do not rewrite multiexpressions in synthetic setter/getters because:
      //   (1) jscompiler has a custom pass to inline these and expects them in a specific way, and
      //   (2) there is no reason to normalize these as there will be no variable declarations in
      //       them.
      return method.getDescriptor().getOrigin() != MethodOrigin.SYNTHETIC_PROPERTY_SETTER
          && method.getDescriptor().getOrigin() != MethodOrigin.SYNTHETIC_PROPERTY_GETTER;
    }

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
          return Block.newBuilder()
              .setSourcePosition(statement.getSourcePosition())
              .setStatements(
                  expressions
                      .stream()
                      .map(
                          innerExpression ->
                              innerExpression.makeStatement(statement.getSourcePosition()))
                      .collect(Collectors.toList()))
              .build();
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

      if (flattenedExpressions.size() == 1) {
        Expression expression = Iterables.getOnlyElement(flattenedExpressions);
        // TODO(b/67753876): All of this would be unnecessary if we parenthesize according to
        // precedence.
        if (areParenthesisUnnecessary(expression)) {
          return expression;
        }
      }
      return MultiExpression.newBuilder().setExpressions(flattenedExpressions).build();
    }
  }

  private static boolean areParenthesisUnnecessary(Expression expression) {
    if (expression instanceof Invocation) {
      // @JsProperty Setters are not safe to unparenthesize, e.g. unparenthesizing
      //     a | (b.f = 1)
      // into
      //     a | b.f = 1
      // results in syntactically incorrect code.
      //
      // That being said, @JsProperty setters are not allowed to return a type other than void;
      // so currently there is no syntactic way to get into this situation.
      return true;
    } else if (expression instanceof FieldAccess) {
      // Field accesses are always safe to unparenthesize.
      return true;
    } else if (expression instanceof Literal) {
      // Literals are always safe to unparenthesize.
      return true;
    } else if (expression instanceof VariableReference) {
      // Variable references are always safe to unparenthesize.
      return true;
    } else if (expression instanceof ArrayAccess) {
      // Array access are always safe to unparenthesize.
      return true;
    } else if (expression instanceof SuperReference) {
      // "super" is always safe to unparenthesize.
      return true;
    } else if (expression instanceof ThisReference) {
      // "this" is always safe to unparenthesize.
      return true;
    } else if (expression instanceof ExpressionWithComment) {
      // expressions with comment are safe to unparenthesize if the underlying expression is.
      return areParenthesisUnnecessary(((ExpressionWithComment) expression).getExpression());
    }
    return false;
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
