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

import static com.google.j2cl.transpiler.backend.wasm.ExpressionTranspiler.returnsVoid;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BreakStatement;
import com.google.j2cl.transpiler.ast.ContinueStatement;
import com.google.j2cl.transpiler.ast.DoWhileStatement;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.ForStatement;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.LoopStatement;
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
        builder.append(";; unimplemented statement " + assertStatement.getClass().getSimpleName());
        return false;
      }

      @Override
      public boolean enterBlock(Block block) {
        builder.openParens("block");
        renderStatements(block.getStatements());
        builder.closeParens();
        return false;
      }

      @Override
      public boolean enterBreakStatement(BreakStatement breakStatement) {
        builder.append(
            "(br " + getBreakLabelName(breakStatement.getLabelReference().getTarget()) + ")");
        return false;
      }

      @Override
      public boolean enterContinueStatement(ContinueStatement continueStatement) {
        builder.append(
            "(br " + getContinueLabelName(continueStatement.getLabelReference().getTarget()) + ")");
        return false;
      }

      @Override
      public boolean enterExpressionStatement(ExpressionStatement expressionStatement) {
        Expression expression = expressionStatement.getExpression();
        builder.emitWithMapping(
            expressionStatement.getSourcePosition(),
            () -> renderExpressionWithUnusedResult(expression));

        return false;
      }

      private void renderExpressionWithUnusedResult(Expression expression) {
        if (returnsVoid(expression)) {
          renderExpression(expression);
        } else {
          builder.append("(drop ");
          renderExpression(expression);
          builder.append(")");
        }
      }

      @Override
      public boolean enterIfStatement(IfStatement ifStatement) {
        builder.openParens("if ");
        builder.emitWithMapping(
            ifStatement.getSourcePosition(),
            () -> renderExpression(ifStatement.getConditionExpression()));
        builder.newLine();
        builder.openParens("then");
        builder.newLine();
        renderStatement(ifStatement.getThenStatement());
        builder.closeParens();
        if (ifStatement.getElseStatement() != null) {
          builder.openParens("else");
          builder.newLine();
          renderStatement(ifStatement.getElseStatement());
          builder.closeParens();
        }
        builder.closeParens();
        return false;
      }

      @Override
      public boolean enterLabeledStatement(LabeledStatement labeledStatement) {
        if (labeledStatement.getStatement() instanceof LoopStatement) {
          // Let the loops handle the labeling themselves.
          return true;
        }
        String label = getBreakLabelName(labeledStatement.getLabel());
        builder.openParens("block " + label);
        builder.newLine();
        renderStatement(labeledStatement.getStatement());
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
        // TODO(rluble): This is a nominal placeholder implementation that throws to JavaScript
        // until WASM exception handling is added.
        builder.emitWithMapping(
            throwStatement.getSourcePosition(), () -> builder.append("unreachable"));
        return false;
      }

      @Override
      public boolean enterWhileStatement(WhileStatement whileStatement) {
        //  (block
        //    (loop
        //      (br_if 1 !cond)  ;; exit loop if condition is not met.
        //      ...body...
        //      (br 0)   ;; repeat the loop
        //    )
        //  )
        //
        renderLoop(
            () -> {
              renderConditionalBranch(
                  whileStatement.getSourcePosition(),
                  whileStatement.getConditionExpression().prefixNot(),
                  1);
              renderLabeledStatement(getContinueLabelName(getLabel()), whileStatement.getBody());
              renderUnconditionalBranch(0);
            });
        return false;
      }

      @Override
      public boolean enterDoWhileStatement(DoWhileStatement doWhileStatement) {
        //  (block
        //    (loop
        //      ...body...
        //      (br_if 0 cond)  ;; loop back if condition is met.
        //    )
        //  )
        //
        renderLoop(
            () -> {
              renderLabeledStatement(getContinueLabelName(getLabel()), doWhileStatement.getBody());
              renderConditionalBranch(
                  doWhileStatement.getSourcePosition(),
                  doWhileStatement.getConditionExpression(),
                  0);
            });
        return false;
      }

      @Override
      public boolean enterForStatement(ForStatement forStatement) {
        //  ...initializers...
        //  (block
        //    (loop
        //      (br_if 1 !cond)  ;; exit loop if condition is not met.
        //      ...body...
        //      ...updates...
        //      (br 0)   ;; repeat the loop
        //    )
        //  )
        //
        forStatement
            .getInitializers()
            .forEach(
                i -> {
                  builder.newLine();
                  renderExpressionWithUnusedResult(i);
                });

        renderLoop(
            () -> {
              renderConditionalBranch(
                  forStatement.getSourcePosition(),
                  forStatement.getConditionExpression().prefixNot(),
                  1);
              renderLabeledStatement(getContinueLabelName(getLabel()), forStatement.getBody());
              forStatement
                  .getUpdates()
                  .forEach(
                      u -> {
                        builder.newLine();
                        renderExpressionWithUnusedResult(u);
                      });
              renderUnconditionalBranch(0);
            });
        return false;
      }

      private void renderUnconditionalBranch(int target) {
        builder.newLine();
        builder.append(String.format("(br %d)", target));
      }

      private void renderConditionalBranch(
          SourcePosition sourcePosition, Expression condition, int target) {
        builder.newLine();
        builder.emitWithMapping(
            sourcePosition,
            () -> {
              builder.append(String.format("(br_if %d ", target));
              renderExpression(condition);
              builder.append(")");
            });
      }

      void renderLoop(Runnable bodyEmitter) {
        builder.newLine();
        builder.openParens("block " + getBreakLabelName(getLabel()));
        builder.newLine();
        builder.openParens("loop");
        bodyEmitter.run();
        builder.newLine();
        builder.closeParens();
        builder.closeParens();
      }

      void renderLabeledStatement(String label, Statement statement) {
        builder.newLine();
        builder.openParens("block " + label);
        builder.newLine();
        renderStatement(statement);
        builder.closeParens();
      }

      private String getBreakLabelName(Label label) {
        return environment.getDeclarationName(label) + ".BREAK";
      }

      private String getContinueLabelName(Label label) {
        return environment.getDeclarationName(label) + ".CONTINUE";
      }

      private Label getLabel() {
        return ((LabeledStatement) getParent()).getLabel();
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
