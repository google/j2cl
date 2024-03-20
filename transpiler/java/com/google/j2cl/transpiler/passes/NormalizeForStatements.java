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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.BreakStatement;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ContinueStatement;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ForStatement;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.WhileStatement;

/**
 * Rewrites ForStatement into WhileStatement, rewriting ContinueStatements in to BreakStatements.
 *
 * <p>Before:
 *
 * <pre>{@code
 * LABEL:
 * for (int i = 0; i < 10; i++) {
 *   if (i == 5) {
 *     continue LABEL;
 *   }
 *   foo();
 * }
 * }</pre>
 *
 * <p>After:
 *
 * <pre>{@code
 * {
 *   int i = 0;
 *   LABEL:
 *   while (i < 10) {
 *     LABEL_CONTINUE: {
 *       if (i == 5) {
 *         break LABEL_CONTINUE;
 *       }
 *       foo();
 *     }
 *     i++;
 *   }
 * }
 * }</pre>
 */
public class NormalizeForStatements extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteLabeledStatement(LabeledStatement labeledStatement) {
            Statement statement = labeledStatement.getStatement();
            return statement instanceof ForStatement
                ? rewriteLabeledForStatement(labeledStatement.getLabel(), (ForStatement) statement)
                : labeledStatement;
          }
        });
  }

  private static Statement rewriteLabeledForStatement(Label forLabel, ForStatement forStatement) {
    Block.Builder rewrittenBlockBuilder =
        Block.newBuilder().setSourcePosition(forStatement.getSourcePosition());

    forStatement
        .getInitializers()
        .forEach(
            initializer ->
                rewrittenBlockBuilder.addStatement(
                    initializer.makeStatement(forStatement.getSourcePosition())));

    Expression forConditionExpression = forStatement.getConditionExpression();

    Expression whileConditionExpression =
        forConditionExpression != null ? forConditionExpression : BooleanLiteral.get(true);

    Block.Builder whileBlockBuilder =
        Block.newBuilder().setSourcePosition(forStatement.getSourcePosition());

    Label continueLabel = Label.newBuilder().setName(forLabel.getName() + "_CONTINUE").build();

    whileBlockBuilder.addStatement(
        LabeledStatement.newBuilder()
            .setLabel(continueLabel)
            .setSourcePosition(forStatement.getSourcePosition())
            .setStatement(
                rewriteContinueStatementsToBreakStatements(
                    forStatement.getBody(), forLabel, continueLabel))
            .build());

    forStatement
        .getUpdates()
        .forEach(
            update ->
                whileBlockBuilder.addStatement(
                    update.makeStatement(forStatement.getSourcePosition())));

    Statement whileStatement =
        WhileStatement.newBuilder()
            .setSourcePosition(forStatement.getSourcePosition())
            .setConditionExpression(whileConditionExpression)
            .setBody(whileBlockBuilder.build())
            .build();

    Statement labeledWhileStatement =
        LabeledStatement.newBuilder()
            .setSourcePosition(whileStatement.getSourcePosition())
            .setLabel(forLabel)
            .setStatement(whileStatement)
            .build();

    rewrittenBlockBuilder.addStatement(labeledWhileStatement);

    return rewrittenBlockBuilder.build();
  }

  private static Statement rewriteContinueStatementsToBreakStatements(
      Statement statement, Label continueLabel, Label breakLabel) {
    return (Statement)
        statement.rewrite(
            new AbstractRewriter() {
              @Override
              public Node rewriteContinueStatement(ContinueStatement continueStatement) {
                return continueStatement.targetsLabel(continueLabel)
                    ? BreakStatement.Builder.from(continueStatement)
                        .setLabelReference(breakLabel.createReference())
                        .build()
                    : continueStatement;
              }
            });
  }
}
