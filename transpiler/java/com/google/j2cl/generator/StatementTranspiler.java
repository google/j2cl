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
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.SwitchCase;
import com.google.j2cl.ast.SwitchStatement;
import com.google.j2cl.ast.SynchronizedStatement;
import com.google.j2cl.ast.ThrowStatement;
import com.google.j2cl.ast.TryStatement;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.WhileStatement;
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

  public void renderStatement(Statement statement) {
    class SourceTransformer extends AbstractVisitor {
      private void render(Node node) {
        node.accept(this);
      }

      @Override
      public boolean enterAssertStatement(AssertStatement assertStatement) {
        builder.emitWithMapping(
            assertStatement.getSourcePosition(),
            () -> {
              String assertAlias = environment.aliasForType(BootstrapType.ASSERTS.getDescriptor());
              if (assertStatement.getMessage() == null) {
                builder.append(assertAlias + ".$assert(");
                renderExpression(assertStatement.getExpression());
                builder.append(");");
              } else {
                builder.append(assertAlias + ".$assertWithMessage(");
                renderExpression(assertStatement.getExpression());
                builder.append(", ");
                renderExpression(assertStatement.getMessage());
                builder.append(");");
              }
            });
        return false;
      }

      @Override
      public boolean enterBlock(Block block) {
        builder.openBrace();
        for (Statement statement : block.getStatements()) {
          builder.newLine();
          render(statement);
        }
        builder.closeBrace();
        return false;
      }

      @Override
      public boolean enterBreakStatement(BreakStatement breakStatement) {
        builder.emitWithMapping(
            breakStatement.getSourcePosition(),
            () -> {
              builder.append("break");
              if (breakStatement.getLabelName() != null) {
                builder.append(" " + breakStatement.getLabelName());
              }
              builder.append(";");
            });
        return false;
      }

      @Override
      public boolean enterCatchClause(CatchClause catchClause) {
        render(catchClause.getBody());
        return false;
      }

      @Override
      public boolean enterContinueStatement(ContinueStatement continueStatement) {
        builder.emitWithMapping(
            continueStatement.getSourcePosition(),
            () -> {
              builder.append("continue");
              if (continueStatement.getLabelName() != null) {
                builder.append(" " + continueStatement.getLabelName());
              }
              builder.append(";");
            });
        return false;
      }

      @Override
      public boolean enterDoWhileStatement(DoWhileStatement doWhileStatement) {
        builder.emitWithMapping(
            doWhileStatement.getSourcePosition(),
            () -> {
              builder.append("do ");
              render(doWhileStatement.getBody());
              builder.append("while (");
              renderExpression(doWhileStatement.getConditionExpression());
              builder.append(");");
            });
        return false;
      }

      @Override
      public boolean enterExpressionStatement(ExpressionStatement expressionStatement) {
        builder.emitWithMapping(
            expressionStatement.getSourcePosition(),
            () -> {
              renderExpression(expressionStatement.getExpression());
              builder.append(";");
            });
        return false;
      }

      @Override
      public boolean enterForStatement(ForStatement forStatement) {
        builder.emitWithMapping(
            forStatement.getSourcePosition(),
            () -> {
              builder.append("for (");
              renderSeparated(", ", forStatement.getInitializers());
              builder.append("; ");
              renderExpression(forStatement.getConditionExpression());
              builder.append("; ");
              renderSeparated(", ", forStatement.getUpdates());
              builder.append(") ");
              render(forStatement.getBody());
            });
        return false;
      }

      @Override
      public boolean enterIfStatement(IfStatement ifStatement) {
        builder.emitWithMapping(
            ifStatement.getSourcePosition(),
            () -> {
              builder.append("if (");
              renderExpression(ifStatement.getConditionExpression());
              builder.append(") ");
              render(ifStatement.getThenStatement());
              if (ifStatement.getElseStatement() != null) {
                builder.append(" else ");
                render(ifStatement.getElseStatement());
              }
            });
        return false;
      }

      @Override
      public boolean enterLabeledStatement(LabeledStatement labelStatement) {
        builder.emitWithMapping(
            labelStatement.getSourcePosition(),
            () -> {
              builder.append(labelStatement.getLabelName() + ": ");
              render(labelStatement.getBody());
            });
        return false;
      }

      @Override
      public boolean enterReturnStatement(ReturnStatement returnStatement) {
        builder.emitWithMapping(
            returnStatement.getSourcePosition(),
            () -> {
              Expression expression = returnStatement.getExpression();
              builder.append("return");
              if (expression != null) {
                builder.append(" ");
                renderExpression(expression);
              }
              builder.append(";");
            });
        return false;
      }

      @Override
      public boolean enterSwitchCase(SwitchCase switchCase) {
        builder.emitWithMapping(
            switchCase.getSourcePosition(),
            () -> {
              if (switchCase.isDefault()) {
                builder.append("default: ");
              } else {
                builder.append("case ");
                renderExpression(switchCase.getMatchExpression());
                builder.append(": ");
              }
            });
        return false;
      }

      @Override
      public boolean enterSwitchStatement(SwitchStatement switchStatement) {
        builder.emitWithMapping(
            switchStatement.getSourcePosition(),
            () -> {
              builder.append("switch (");
              renderExpression(switchStatement.getSwitchExpression());
              builder.append(") ");
              builder.openBrace();
              for (Statement statement : switchStatement.getBodyStatements()) {
                if (statement instanceof SwitchCase) {
                  builder.newLine();
                  render(statement);
                } else {
                  builder.indent();
                  builder.newLine();
                  render(statement);
                  builder.unindent();
                }
              }
              builder.closeBrace();
            });
        return false;
      }

      @Override
      public boolean enterSynchronizedStatement(SynchronizedStatement synchronizedStatement) {
        builder.emitWithMapping(
            synchronizedStatement.getSourcePosition(),
            () -> {
              String utilAlias =
                  environment.aliasForType(BootstrapType.NATIVE_UTIL.getDescriptor());
              builder.append(utilAlias + ".$synchronized(");
              renderExpression(synchronizedStatement.getExpression());
              builder.appendln(");");
              render(synchronizedStatement.getBody());
            });
        return false;
      }

      @Override
      public boolean enterThrowStatement(ThrowStatement throwStatement) {
        builder.emitWithMapping(
            throwStatement.getSourcePosition(),
            () -> {
              builder.append("throw ");
              renderExpression(throwStatement.getExpression());
              builder.append(";");
            });
        return false;
      }

      @Override
      public boolean enterTryStatement(TryStatement tryStatement) {
        builder.emitWithMapping(
            tryStatement.getSourcePosition(),
            () -> {
              builder.append("try ");
              render(tryStatement.getBody());
              for (CatchClause catchClause : tryStatement.getCatchClauses()) {
                builder.append(
                    " catch (/** @type {*} */ "
                        + environment.aliasForVariable(catchClause.getExceptionVar())
                        + ") ");
                render(catchClause.getBody());
              }
              if (tryStatement.getFinallyBlock() != null) {
                builder.append(" finally ");
                render(tryStatement.getFinallyBlock());
              }
            });
        return false;
      }

      @Override
      public boolean enterWhileStatement(WhileStatement whileStatement) {
        builder.emitWithMapping(
            whileStatement.getSourcePosition(),
            () -> {
              builder.append("while (");
              renderExpression(whileStatement.getConditionExpression());
              builder.append(") ");
              render(whileStatement.getBody());
            });
        return false;
      }

      @Override
      public String toString() {
        return builder.build();
      }

      private void renderExpression(Expression expression) {
        if (expression == null) {
          return;
        }
        ExpressionTranspiler.render(expression, environment, builder);
      }

      private void renderSeparated(String separator, List<? extends Expression> expressions) {
        String nextSeparator = "";
        for (Expression expression : expressions) {
          builder.append(nextSeparator);
          nextSeparator = separator;
          renderExpression(expression);
        }
      }
    }

    statement.accept(new SourceTransformer());
  }
}
