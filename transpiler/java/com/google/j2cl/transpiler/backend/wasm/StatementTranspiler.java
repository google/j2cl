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
package com.google.j2cl.transpiler.backend.wasm;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.j2cl.transpiler.backend.wasm.ExpressionTranspiler.returnsVoid;

import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.BreakStatement;
import com.google.j2cl.transpiler.ast.ContinueStatement;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.LabelReference;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.ThrowStatement;
import com.google.j2cl.transpiler.ast.WhileStatement;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.util.Collection;

/** Transforms Statements into WASM code. */
class StatementTranspiler {
  SourceBuilder builder;
  GenerationEnvironment environment;

  public StatementTranspiler(SourceBuilder builder, GenerationEnvironment environment) {
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

      @Override
      public boolean enterStatement(Statement assertStatement) {
        builder.appendln(
            ";; unimplemented statement " + assertStatement.getClass().getSimpleName());
        return false;
      }

      @Override
      public boolean enterBlock(Block block) {
        builder.openParens();
        builder.append("block");
        renderStatements(block.getStatements());
        builder.closeParens();
        return false;
      }

      @Override
      public boolean enterBreakStatement(BreakStatement breakStatement) {
        renderBranchStatement(breakStatement.getLabelReference());
        return false;
      }

      @Override
      public boolean enterContinueStatement(ContinueStatement continueStatement) {
        renderBranchStatement(continueStatement.getLabelReference());
        return false;
      }

      private void renderBranchStatement(LabelReference labelReference) {
        checkNotNull(labelReference);
        builder.appendln("(br " + environment.getDeclarationName(labelReference.getTarget()) + ")");
      }

      @Override
      public boolean enterExpressionStatement(ExpressionStatement expressionStatement) {
        Expression expression = expressionStatement.getExpression();
        builder.emitWithMapping(
            expressionStatement.getSourcePosition(),
            () -> {
              renderExpression(expression);
            });
        if (!returnsVoid(expression)) {
          builder.newLine();
          // Remove the result of the expression from the stack.
          builder.append("drop");
        }
        return false;
      }

      @Override
      public boolean enterIfStatement(IfStatement ifStatement) {
        builder.append("(if ");
        builder.emitWithMapping(
            ifStatement.getSourcePosition(),
            () -> renderExpression(ifStatement.getConditionExpression()));
        builder.append(" (then ");
        renderStatement(ifStatement.getThenStatement());
        builder.append(")");
        if (ifStatement.getElseStatement() != null) {
          builder.append(" (else ");
          renderStatement(ifStatement.getElseStatement());
          builder.append(")");
        }
        builder.append(")");
        return false;
      }

      @Override
      public boolean enterLabeledStatement(LabeledStatement labeledStatement) {
        builder.openParens();
        String label = environment.getDeclarationName(labeledStatement.getLabel());

        if (labeledStatement.getStatement() instanceof WhileStatement) {
          WhileStatement whileStatement = (WhileStatement) labeledStatement.getStatement();
          checkState(whileStatement.getConditionExpression().equals(BooleanLiteral.get(true)));
          builder.appendln("loop " + label + " ");
          renderStatement(((WhileStatement) labeledStatement.getStatement()).getBody());
        } else {
          builder.appendln("block " + label + " ");
          renderStatement(labeledStatement.getStatement());
        }
        builder.closeParens();
        return false;
      }

      @Override
      public boolean enterReturnStatement(ReturnStatement returnStatement) {
        builder.emitWithMapping(
            returnStatement.getSourcePosition(),
            () -> {
              if (returnStatement.getExpression() != null) {
                builder.append("(local.set $return.value ");
                ExpressionTranspiler.renderTypedExpression(
                    returnStatement.getTypeDescriptor(),
                    returnStatement.getExpression(),
                    builder,
                    environment);
                builder.append(")");
                builder.newLine();
              }
              builder.append("(br $return.label)");
            });
        return false;
      }

      @Override
      public boolean enterThrowStatement(ThrowStatement throwStatement) {
        renderExpression(throwStatement.getExpression());
        builder.newLine();
        // TODO(rluble): This is a nominal placeholder implementation that throws to JavaScript
        // until WASM exception handling is added.
        builder.emitWithMapping(
            throwStatement.getSourcePosition(), () -> builder.append("unreachable"));
        return false;
      }

      private void renderExpression(Expression expression) {
        ExpressionTranspiler.render(expression, builder, environment);
      }
    }

    renderFirstLineAsComment(statement);
    statement.accept(new SourceTransformer());
  }

  /** Render first line of the source code for {@code statement} as a WASM comment. * */
  private void renderFirstLineAsComment(Statement s) {
    if (s instanceof Block) {
      return;
    }
    String[] parts = s.toString().split("\n", 2);
    builder.append(";; ");
    builder.append(parts[0]);
    builder.newLine();
  }
}
