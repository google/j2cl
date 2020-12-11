/*
 * Copyright 2020 Google Inc.
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

import static java.util.stream.Collectors.toList;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.ForStatement;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.OperationExpansionUtils;
import java.util.HashSet;
import java.util.Set;

/**
 * Expands assignments from assignment expressions so that they are no longer used as an expression.
 */
public class RewriteAssignmentExpressions extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    /** List of assignment expressions where the result is not used */
    Set<Expression> unusedResultAssignments = new HashSet<>();

    // Collect assignment expressions where the result is not used.
    compilationUnit.accept(
        new AbstractVisitor() {

          @Override
          public void exitExpressionStatement(ExpressionStatement statement) {
            if (isAssignmentExpression(statement.getExpression())) {
              unusedResultAssignments.add(statement.getExpression());
            }
          }

          // TODO(dramaix): remove this method when for loops are rewritten as while loops.
          @Override
          public void exitForStatement(ForStatement forStatement) {
            unusedResultAssignments.addAll(
                Streams.concat(
                        forStatement.getInitializers().stream(), forStatement.getUpdates().stream())
                    .filter(RewriteAssignmentExpressions::isAssignmentExpression)
                    .collect(toList()));
          }

          @Override
          public void exitMultiExpression(MultiExpression multiExpression) {
            Expression lastExpression = Iterables.getLast(multiExpression.getExpressions());
            unusedResultAssignments.addAll(
                multiExpression.getExpressions().stream()
                    .filter(e -> isAssignmentExpression(e) && e != lastExpression)
                    .collect(toList()));
          }
        });

    // Expand all assignment expressions where the result is used.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression expression) {
            if (expression.getOperator() != BinaryOperator.ASSIGN
                || unusedResultAssignments.contains(expression)) {
              return expression;
            }

            return OperationExpansionUtils.expandAssignmentExpression(expression);
          }
        });
  }

  private static boolean isAssignmentExpression(Expression expression) {
    return expression instanceof BinaryExpression
        && ((BinaryExpression) expression).getOperator() == BinaryOperator.ASSIGN;
  }
}
