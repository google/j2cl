/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.generator;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.BreakStatement;
import com.google.j2cl.ast.CatchClause;
import com.google.j2cl.ast.ContinueStatement;
import com.google.j2cl.ast.DoWhileStatement;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.ForStatement;
import com.google.j2cl.ast.IfStatement;
import com.google.j2cl.ast.LabeledStatement;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.SwitchCase;
import com.google.j2cl.ast.SwitchStatement;
import com.google.j2cl.ast.SynchronizedStatement;
import com.google.j2cl.ast.ThrowStatement;
import com.google.j2cl.ast.TryStatement;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.WhileStatement;
import com.google.j2cl.ast.sourcemap.SourceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Transforms Statements to JavaScript source strings.
 */
public class StatementTranspiler {
  SourceBuilder builder;
  GenerationEnvironment environment;

  public StatementTranspiler(SourceBuilder builder, GenerationEnvironment environment) {
    this.builder = builder;
    this.environment = environment;
  }

  public void renderStatement(Statement node) {
    class SourceTransformer extends AbstractVisitor {
      @Override
      public boolean enterStatement(Statement statement) {
        return true;
      }

      @Override
      public boolean enterAssertStatement(AssertStatement assertStatement) {
        String assertAlias = environment.aliasForType(BootstrapType.ASSERTS.getDescriptor());
        String line;
        if (assertStatement.getMessage() == null) {
          line =
              String.format(
                  "%s.$enabled() && %s.$assert(%s);",
                  assertAlias,
                  assertAlias,
                  toSourceExpression(assertStatement.getExpression()));
        } else {
          line =
              String.format(
                  "%s.$enabled() && %s.$assertWithMessage(%s, %s);",
                  assertAlias,
                  assertAlias,
                  toSourceExpression(assertStatement.getExpression()),
                  toSourceExpression(assertStatement.getMessage()));
        }
        SourceInfo location = builder.appendln(line);
        assertStatement.setOutputSourceInfo(location);
        return false;
      }

      @Override
      public boolean enterBlock(Block blockStatement) {
        builder.appendln("{");
        return true; // Allow the visitor to visit the contained statements.
      }

      @Override
      public boolean enterBreakStatement(BreakStatement breakStatement) {
        SourceInfo location;
        if (breakStatement.getLabelName() == null) {
          location = builder.appendln("break;");
        } else {
          location = builder.appendln(String.format("break %s;", breakStatement.getLabelName()));
        }
        breakStatement.setOutputSourceInfo(location);
        return false;
      }

      @Override
      public boolean enterCatchClause(CatchClause catchClause) {
        catchClause.getBody().accept(this);
        return super.enterCatchClause(catchClause);
      }

      @Override
      public boolean enterContinueStatement(ContinueStatement continueStatement) {
        SourceInfo location;
        if (continueStatement.getLabelName() == null) {
          location = builder.appendln("continue;");
        } else {
          location =
              builder.appendln(String.format("continue %s;", continueStatement.getLabelName()));
        }
        continueStatement.setOutputSourceInfo(location);
        return false;
      }

      @Override
      public boolean enterDoWhileStatement(DoWhileStatement doWhileStatement) {
        SourceInfo location = builder.append("do ");
        doWhileStatement.getBody().accept(this);
        String conditionAsString = toSourceExpression(doWhileStatement.getConditionExpression());
        builder.appendln(String.format("while(%s);", conditionAsString));
        doWhileStatement.setOutputSourceInfo(location);
        return false;
      }

      @Override
      public boolean enterExpressionStatement(ExpressionStatement expressionStatement) {
        SourceInfo location =
            builder.appendln(toSourceExpression(expressionStatement.getExpression()) + ";");
        expressionStatement.setOutputSourceInfo(location);
        return false;
      }

      @Override
      public boolean enterForStatement(ForStatement forStatement) {
        List<String> initializers = new ArrayList<>();
        for (Expression e : forStatement.getInitializers()) {
          initializers.add(toSourceExpression(e));
        }
        String initializerAsString = Joiner.on(",").join(initializers);

        String conditionExpressionAsString =
            forStatement.getConditionExpression() == null
                ? ""
                : toSourceExpression(forStatement.getConditionExpression());

        List<String> updaters = new ArrayList<>();
        for (Expression e : forStatement.getUpdates()) {
          updaters.add(toSourceExpression(e));
        }
        String updatersAsString = Joiner.on(",").join(updaters);
        SourceInfo location =
            builder.appendln(
                String.format(
                    "for (%s; %s; %s)",
                    initializerAsString,
                    conditionExpressionAsString,
                    updatersAsString));
        forStatement.setOutputSourceInfo(location);
        forStatement.getBody().accept(this);
        return false;
      }

      @Override
      public boolean enterIfStatement(IfStatement ifStatement) {
        SourceInfo location =
            builder.append(
                String.format("if (%s)", toSourceExpression(ifStatement.getConditionExpression())));
        ifStatement.setOutputSourceInfo(location);
        ifStatement.getThenStatement().accept(this);
        if (ifStatement.getElseStatement() != null) {
          builder.append(" else ");
          ifStatement.getElseStatement().accept(this);
        }
        return false;
      }

      @Override
      public boolean enterLabeledStatement(LabeledStatement labelStatement) {
        SourceInfo location = builder.append(String.format("%s: ", labelStatement.getLabelName()));
        labelStatement.setOutputSourceInfo(location);
        labelStatement.getBody().accept(this);
        return false;
      }

      @Override
      public boolean enterReturnStatement(ReturnStatement returnStatement) {
        SourceInfo location = builder.append("return");
        returnStatement.setOutputSourceInfo(location);
        Expression expression = returnStatement.getExpression();
        if (expression != null) {
          builder.append(" " + toSourceExpression(expression));
        }
        builder.appendln(";");
        return false;
      }

      @Override
      public boolean enterSwitchCase(SwitchCase switchCase) {
        SourceInfo location;
        if (switchCase.isDefault()) {
          location = builder.appendln("default:");
        } else {
          location =
              builder.appendln("case " + toSourceExpression(switchCase.getMatchExpression()) + ":");
        }
        switchCase.setOutputSourceInfo(location);
        return false;
      }

      @Override
      public boolean enterSwitchStatement(SwitchStatement switchStatement) {
        SourceInfo location =
            builder.appendln(
                String.format(
                    "switch (%s) {", toSourceExpression(switchStatement.getMatchExpression())));
        switchStatement.setOutputSourceInfo(location);
        return true; // Allow the visitor to enter the switch cases.
      }

      @Override
      public boolean enterSynchronizedStatement(SynchronizedStatement synchronizedStatement) {
        builder.appendln(toSourceExpression(synchronizedStatement.getExpression()) + ";");
        synchronizedStatement.getBody().accept(this);
        return false;
      }

      @Override
      public boolean enterThrowStatement(ThrowStatement throwStatement) {
        SourceInfo location =
            builder.appendln("throw " + toSourceExpression(throwStatement.getExpression()) + ";");
        throwStatement.setOutputSourceInfo(location);
        return false;
      }

      @Override
      public boolean enterTryStatement(TryStatement tryStatement) {
        SourceInfo location = builder.append("try");
        tryStatement.setOutputSourceInfo(location);
        tryStatement.getBody().accept(this);
        // Generate catch clause.
        Preconditions.checkArgument(tryStatement.getCatchClauses().size() < 2);
        if (tryStatement.getCatchClauses().size() == 1) {
          CatchClause catchClause = tryStatement.getCatchClauses().get(0);
          builder.appendln(
              String.format(
                  "catch (/** @type {*} */ %s)",
                  environment.aliasForVariable(catchClause.getExceptionVar())));
          catchClause.getBody().accept(this);
        }
        // Generate finally block.
        if (tryStatement.getFinallyBlock() != null) {
          builder.append("finally");
          tryStatement.getFinallyBlock().accept(this);
        }
        return false;
      }

      @Override
      public boolean enterWhileStatement(WhileStatement whileStatement) {
        String conditionAsString = toSourceExpression(whileStatement.getConditionExpression());
        SourceInfo location = builder.append(String.format("while (%s)", conditionAsString));
        whileStatement.setOutputSourceInfo(location);
        return true; // Allow this to enter block.
      }

      @Override
      public void exitBlock(Block blockStatement) {
        SourceInfo location = builder.appendln("}");
        blockStatement.setOutputSourceInfo(location);
      }

      @Override
      public void exitSwitchStatement(SwitchStatement node) {
        builder.appendln("}");
      }

      private String toSourceExpression(Expression expression) {
        return ExpressionTranspiler.transform(expression, environment);
      }

      @Override
      public String toString() {
        return builder.build();
      }
    }
    SourceTransformer transformer = new SourceTransformer();
    node.accept(transformer);
  }
}
