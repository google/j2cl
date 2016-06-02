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

import com.google.debugging.sourcemap.FilePosition;
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
import com.google.j2cl.ast.sourcemap.SourcePosition;

import java.util.List;

/**
 * Transforms Statements to JavaScript source strings.
 */
public class StatementTranspiler {
  SourceBuilder builder;
  SourceMapBuilder sourceMapBuilder;
  GenerationEnvironment environment;

  public StatementTranspiler(
      SourceBuilder builder, SourceMapBuilder sourceMapBuilder, GenerationEnvironment environment) {
    this.builder = builder;
    this.environment = environment;
    this.sourceMapBuilder = sourceMapBuilder;
  }

  public void renderStatement(Statement statement) {
    class SourceTransformer extends AbstractVisitor {
      private void render(Node node) {
        node.accept(this);
      }

      private String toSeparatedExpressions(String separator, List<Expression> expressions) {
        StringBuilder sb = new StringBuilder();
        String nextSeparator = "";
        for (Expression expression : expressions) {
          sb.append(nextSeparator);
          nextSeparator = separator;
          sb.append(toSourceExpression(expression));
        }
        return sb.toString();
      }

      @Override
      public boolean enterAssertStatement(AssertStatement assertStatement) {
        FilePosition startPosition = builder.getCurrentPosition();

        String assertAlias = environment.aliasForType(BootstrapType.ASSERTS.getDescriptor());
        builder.append(assertAlias + ".$enabled() && ");
        if (assertStatement.getMessage() == null) {
          builder.append(
              assertAlias
                  + ".$assert("
                  + toSourceExpression(assertStatement.getExpression())
                  + ");");
        } else {
          builder.append(
              assertAlias
                  + ".$assertWithMessage("
                  + toSourceExpression(assertStatement.getExpression())
                  + ", "
                  + toSourceExpression(assertStatement.getMessage())
                  + ");");
        }
        addSourceMapping(assertStatement, startPosition, builder.getCurrentPosition());
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
        FilePosition startPosition = builder.getCurrentPosition();

        builder.append("break");
        if (breakStatement.getLabelName() != null) {
          builder.append(" " + breakStatement.getLabelName());
        }
        builder.append(";");

        addSourceMapping(breakStatement, startPosition, builder.getCurrentPosition());
        return false;
      }

      @Override
      public boolean enterCatchClause(CatchClause catchClause) {
        render(catchClause.getBody());
        return false;
      }

      @Override
      public boolean enterContinueStatement(ContinueStatement continueStatement) {
        FilePosition startPosition = builder.getCurrentPosition();
        builder.append("continue");
        if (continueStatement.getLabelName() != null) {
          builder.append(" " + continueStatement.getLabelName());
        }
        builder.append(";");
        addSourceMapping(continueStatement, startPosition, builder.getCurrentPosition());
        return false;
      }

      @Override
      public boolean enterDoWhileStatement(DoWhileStatement doWhileStatement) {
        FilePosition startPosition = builder.getCurrentPosition();
        builder.append("do ");
        render(doWhileStatement.getBody());
        builder.append("while (");
        builder.append(toSourceExpression(doWhileStatement.getConditionExpression()));
        builder.append(");");
        addSourceMapping(doWhileStatement, startPosition, builder.getCurrentPosition());
        return false;
      }

      @Override
      public boolean enterExpressionStatement(ExpressionStatement expressionStatement) {
        FilePosition startPosition = builder.getCurrentPosition();
        builder.append(toSourceExpression(expressionStatement.getExpression()) + ";");
        addSourceMapping(expressionStatement, startPosition, builder.getCurrentPosition());
        return false;
      }

      @Override
      public boolean enterForStatement(ForStatement forStatement) {
        FilePosition startPosition = builder.getCurrentPosition();
        builder.append("for (");
        builder.append(toSeparatedExpressions(", ", forStatement.getInitializers()));
        builder.append("; ");
        builder.append(toSourceExpression(forStatement.getConditionExpression()));
        builder.append("; ");
        builder.append(toSeparatedExpressions(", ", forStatement.getUpdates()));
        builder.append(") ");
        render(forStatement.getBody());
        addSourceMapping(forStatement, startPosition, builder.getCurrentPosition());
        return false;
      }

      @Override
      public boolean enterIfStatement(IfStatement ifStatement) {
        FilePosition startPosition = builder.getCurrentPosition();
        builder.append("if (");
        builder.append(toSourceExpression(ifStatement.getConditionExpression()));
        builder.append(") ");
        render(ifStatement.getThenStatement());
        if (ifStatement.getElseStatement() != null) {
          builder.append(" else ");
          render(ifStatement.getElseStatement());
        }
        addSourceMapping(ifStatement, startPosition, builder.getCurrentPosition());
        return false;
      }

      @Override
      public boolean enterLabeledStatement(LabeledStatement labelStatement) {
        FilePosition startPosition = builder.getCurrentPosition();
        builder.append(labelStatement.getLabelName() + ": ");
        render(labelStatement.getBody());
        addSourceMapping(labelStatement, startPosition, builder.getCurrentPosition());
        return false;
      }

      @Override
      public boolean enterReturnStatement(ReturnStatement returnStatement) {
        FilePosition startPosition = builder.getCurrentPosition();
        Expression expression = returnStatement.getExpression();
        builder.append(
            "return" + (expression == null ? "" : (" " + toSourceExpression(expression))) + ";");
        addSourceMapping(returnStatement, startPosition, builder.getCurrentPosition());
        return false;
      }

      @Override
      public boolean enterSwitchCase(SwitchCase switchCase) {
        FilePosition startPosition = builder.getCurrentPosition();
        if (switchCase.isDefault()) {
          builder.append("default: ");
        } else {
          builder.append("case " + toSourceExpression(switchCase.getMatchExpression()) + ": ");
        }
        addSourceMapping(switchCase, startPosition, builder.getCurrentPosition());
        return false;
      }

      @Override
      public boolean enterSwitchStatement(SwitchStatement switchStatement) {
        FilePosition startPosition = builder.getCurrentPosition();
        builder.append(
            "switch (" + toSourceExpression(switchStatement.getSwitchExpression()) + ")");
        builder.openBrace();
        builder.indent();
        for (Statement statement : switchStatement.getBodyStatements()) {
          if (statement instanceof SwitchCase) {
            builder.unindent();
          }
          builder.newLine();
          render(statement);
          if (statement instanceof SwitchCase) {
            builder.indent();
          }
        }
        builder.unindent();
        builder.closeBrace();
        addSourceMapping(switchStatement, startPosition, builder.getCurrentPosition());
        return false;
      }

      @Override
      public boolean enterSynchronizedStatement(SynchronizedStatement synchronizedStatement) {
        FilePosition startPosition = builder.getCurrentPosition();
        builder.append(toSourceExpression(synchronizedStatement.getExpression()) + ";");
        builder.newLine();
        render(synchronizedStatement.getBody());
        addSourceMapping(synchronizedStatement, startPosition, builder.getCurrentPosition());
        return false;
      }

      @Override
      public boolean enterThrowStatement(ThrowStatement throwStatement) {
        FilePosition startPosition = builder.getCurrentPosition();
        builder.append("throw " + toSourceExpression(throwStatement.getExpression()) + ";");
        addSourceMapping(throwStatement, startPosition, builder.getCurrentPosition());
        return false;
      }

      @Override
      public boolean enterTryStatement(TryStatement tryStatement) {
        FilePosition startPosition = builder.getCurrentPosition();
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
        addSourceMapping(tryStatement, startPosition, builder.getCurrentPosition());
        return false;
      }

      @Override
      public boolean enterWhileStatement(WhileStatement whileStatement) {
        FilePosition startPosition = builder.getCurrentPosition();
        String conditionAsString = toSourceExpression(whileStatement.getConditionExpression());
        builder.append("while (" + conditionAsString + ") ");
        render(whileStatement.getBody());
        addSourceMapping(whileStatement, startPosition, builder.getCurrentPosition());
        return false;
      }

      @Override
      public String toString() {
        return builder.build();
      }

      private String toSourceExpression(Expression expression) {
        if (expression == null) {
          return "";
        }
        return ExpressionTranspiler.transform(expression, environment);
      }

      private void addSourceMapping(
          Statement statement, FilePosition startPosition, FilePosition endPosition) {
        sourceMapBuilder.addMapping(
            statement.getSourcePosition(), new SourcePosition(startPosition, endPosition));
      }
    }
    SourceTransformer transformer = new SourceTransformer();
    statement.accept(transformer);
  }
}
