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
package com.google.j2cl.transpiler.backend.closure;

import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.AssertStatement;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BreakStatement;
import com.google.j2cl.transpiler.ast.CatchClause;
import com.google.j2cl.transpiler.ast.ContinueStatement;
import com.google.j2cl.transpiler.ast.DoWhileStatement;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.FieldDeclarationStatement;
import com.google.j2cl.transpiler.ast.ForStatement;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.SynchronizedStatement;
import com.google.j2cl.transpiler.ast.ThrowStatement;
import com.google.j2cl.transpiler.ast.TryStatement;
import com.google.j2cl.transpiler.ast.WhileStatement;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Transforms Statements to JavaScript source strings.
 */
public class StatementTranspiler {
  SourceBuilder builder;
  ClosureGenerationEnvironment environment;

  public StatementTranspiler(SourceBuilder builder, ClosureGenerationEnvironment environment) {
    this.builder = builder;
    this.environment = environment;
  }

  public void renderStatements(Collection<Statement> statements) {
    statements.forEach(
        s -> {
          builder.newLine();
          renderStatement(s);
        });
  }

  public void renderStatement(Statement statement) {
    class SourceTransformer extends AbstractVisitor {
      private void render(Node node) {
        node.accept(this);
      }

      @Override
      public boolean enterAssertStatement(AssertStatement assertStatement) {
        throw new InternalCompilerError("AssertStatement should have been normalized away.");
      }

      @Override
      public boolean enterBlock(Block block) {
        builder.openBrace();
        renderStatements(block.getStatements());
        builder.closeBrace();
        return false;
      }

      @Override
      public boolean enterBreakStatement(BreakStatement breakStatement) {
        builder.emitWithMapping(
            breakStatement.getSourcePosition(),
            () -> {
              builder.append("break");
              if (breakStatement.getLabelReference() != null) {
                builder.append(" " + breakStatement.getLabelReference().getTarget().getName());
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
              if (continueStatement.getLabelReference() != null) {
                builder.append(" " + continueStatement.getLabelReference().getTarget().getName());
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
              builder.append(" while (");
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
      public boolean enterFieldDeclarationStatement(FieldDeclarationStatement declaration) {
        String typeJsDoc =
            environment.getClosureTypeString(declaration.getFieldDescriptor().getTypeDescriptor());
        ArrayList<String> jsDocs = new ArrayList<>();
        if (!declaration.isPublic()) {
          jsDocs.add("@private");
        }
        if (declaration.isConst()) {
          jsDocs.add("@const");
        }
        if (jsDocs.isEmpty()) {
          jsDocs.add("@type");
        }
        jsDocs.add("{" + typeJsDoc + "}");
        if (declaration.isDeprecated()) {
          jsDocs.add("@deprecated");
        }
        if (!declaration.getFieldDescriptor().canBeReferencedExternally()) {
          jsDocs.add("@nodts");
        }
        Runnable renderer =
            () ->
                builder.emitWithMapping(
                    declaration.getSourcePosition(),
                    () -> {
                      builder.appendln("/**" + String.join(" ", jsDocs) + "*/");
                      renderExpression(declaration.getExpression());
                      builder.append(";");
                    });

        if (declaration.getFieldDescriptor().isStatic()) {
          // Only emit the member mapping for static fields. LibraryInfoBuilder collects all member
          // mappings and for practical reasons instance fields are not collected.
          builder.emitWithMemberMapping(declaration.getFieldDescriptor(), renderer);
        } else {
          renderer.run();
        }
        return false;
      }

      @Override
      public boolean enterLabeledStatement(LabeledStatement labelStatement) {
        builder.emitWithMapping(
            labelStatement.getSourcePosition(),
            () -> {
              builder.append(labelStatement.getLabel().getName() + ": ");

              Statement innerStatement = labelStatement.getStatement();
              // TODO(b/174246745): Remove block braces once the underlying jscompiler bug is fixed.
              if (innerStatement instanceof LabeledStatement) {
                builder.openBrace();
              }
              render(innerStatement);
              if (innerStatement instanceof LabeledStatement) {
                builder.closeBrace();
              }
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
        if (switchCase.isDefault()) {
          builder.append("default: ");
        } else {
          builder.append("case ");
          renderExpression(switchCase.getCaseExpression());
          builder.append(": ");
        }
        builder.indent();
        renderStatements(switchCase.getStatements());
        builder.unindent();
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
              for (SwitchCase switchcase : switchStatement.getCases()) {
                builder.newLine();
                render(switchcase);
              }
              builder.closeBrace();
            });
        return false;
      }

      @Override
      public boolean enterSynchronizedStatement(SynchronizedStatement synchronizedStatement) {
        throw new InternalCompilerError("SynchronizedStatement should have been normalized away.");
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
                    " catch ("
                        + environment.getUniqueNameForVariable(catchClause.getExceptionVariable())
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
