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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
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
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.SynchronizedStatement;
import com.google.j2cl.transpiler.ast.ThrowStatement;
import com.google.j2cl.transpiler.ast.TryStatement;
import com.google.j2cl.transpiler.ast.WhileStatement;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.util.List;

/** Transforms Statements into WASM code. */
class StatementTranspiler {

  public static void render(
      Statement statement, final SourceBuilder builder, final GenerationEnvironment environment) {

    class SourceTransformer extends AbstractVisitor {
      @Override
      public boolean enterStatement(Statement statement) {
        throw new IllegalStateException("Unhandled statement " + statement);
      }

      @Override
      public boolean enterBlock(Block block) {
        builder.openParens("block");
        renderStatements(block.getStatements());
        builder.closeParens();
        return false;
      }

      private void renderStatements(List<Statement> statements) {
        statements.forEach(
            s -> {
              builder.newLine();
              render(s);
            });
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
            () -> ExpressionTranspiler.renderWithUnusedResult(expression, builder, environment));

        return false;
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
        render(ifStatement.getThenStatement());
        builder.closeParens();
        if (ifStatement.getElseStatement() != null) {
          builder.openParens("else");
          builder.newLine();
          render(ifStatement.getElseStatement());
          builder.closeParens();
        }
        builder.closeParens();
        return false;
      }

      @Override
      public boolean enterLabeledStatement(LabeledStatement labeledStatement) {
        if (labeledStatement.getStatement() instanceof LoopStatement) {
          // Let the loops handle the labeling themselves since the need to emit
          // the target for the continue statement.
          return true;
        }
        String label = getBreakLabelName(labeledStatement.getLabel());
        builder.openParens("block " + label);
        builder.newLine();
        render(labeledStatement.getStatement());
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
                // TODO(b/182436577): Replace with renderExpression once renderTypedExpression
                // is removed.
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
      public boolean enterSwitchStatement(SwitchStatement switchStatement) {
        // Switch statements are emitted as a series of nested blocks, with the innermost block
        // corresponding to the first switch case, e.g. code like
        //
        //  switch (e) {
        //    case A:
        //    default:
        //    case B:
        //  }
        //
        // is emitted as:
        //
        // (block ;; case B:
        //   (block ;; default
        //     (block ;; case A:
        //       (block  ;; determine case.
        //         (br_if 0 e != A) ;; skip the rest of conditions and jump to the code for the
        //                          ;; case.
        //         (br_if 2 e != B)
        //         (br 1) ;; jump to the code that handles the default case
        //       )
        //       ... code for case A:
        //     )
        //     ... code for default.
        //   )
        //   ... code for case B:
        // )
        //

        // Open blocks for each case statement.
        for (SwitchCase unused : switchStatement.getCases()) {
          builder.newLine();
          builder.openParens("block");
        }

        renderSwitchDispatchTable(switchStatement);

        // Emit the code for each of the cases.
        for (SwitchCase switchCase : switchStatement.getCases()) {
          builder.newLine();
          builder.append(
              switchCase.isDefault()
                  ? ";; default:"
                  : ";; case " + switchCase.getCaseExpression() + ":");
          renderStatements(switchCase.getStatements());
          builder.closeParens();
        }
        return false;
      }

      private void renderSwitchDispatchTable(SwitchStatement switchStatement) {
        // Evaluate the switch expression and jump to the right case.
        builder.newLine();
        builder.openParens("block ;; evaluate expression and jump");

        // TODO(b/179956682): emit more efficient code for int enums using br_table
        // or binary search.

        int defaultCaseNumber = -1;
        for (int caseNumber = 0; caseNumber < switchStatement.getCases().size(); caseNumber++) {
          // Emit conditions for each case.
          SwitchCase switchCase = switchStatement.getCases().get(caseNumber);
          if (switchCase.isDefault()) {
            // Skip the default case, since all the other conditions need to be evaluated before,
            // but record where the default handling is done (which is not necessarily the last).
            defaultCaseNumber = caseNumber;
            continue;
          }
          // If the condition for this case is met, jump to the start of the case, i.e. jump out
          // of all of the previous enclosing blocks.
          renderConditionalBranch(
              switchStatement.getSourcePosition(),
              switchStatement.getSwitchExpression().infixEquals(switchCase.getCaseExpression()),
              caseNumber);
        }

        // When no other condition was met, jump to the default case if exists.
        renderUnconditionalBranch(
            defaultCaseNumber != -1 ? defaultCaseNumber : switchStatement.getCases().size());
        builder.closeParens();
      }

      @Override
      public boolean enterSynchronizedStatement(SynchronizedStatement synchronizedStatement) {
        render(
            synchronizedStatement
                .getExpression()
                .makeStatement(synchronizedStatement.getSourcePosition()));
        render(synchronizedStatement.getBody());
        return false;
      }

      @Override
      public boolean enterThrowStatement(ThrowStatement throwStatement) {
        builder.newLine();
        builder.emitWithMapping(
            throwStatement.getSourcePosition(),
            () -> {
              builder.append("(throw $exception.event ");
              renderExpression(throwStatement.getExpression());
              builder.append(")");
            });
        return false;
      }

      @Override
      public boolean enterTryStatement(TryStatement tryStatement) {
        // Minimalistic render of try statements.

        render(tryStatement.getBody());
        if (tryStatement.getFinallyBlock() != null) {
          render(tryStatement.getFinallyBlock());
        }
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
                  ExpressionTranspiler.renderWithUnusedResult(i, builder, environment);
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
                        ExpressionTranspiler.renderWithUnusedResult(u, builder, environment);
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
        if (condition.equals(BooleanLiteral.get(false))) {
          // Do not emit the conditional exit if it will never be taken. This covers cases like:
          //    while (true) { ... }
          // Removing this condition in these cases brings the WASM verifier to be inline with
          // the static analysis performed by Java and allows us to avoid inserting unnecessary
          // (unreachable) operations.
          return;
        }
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
        render(statement);
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

      void render(Statement stmt) {
        StatementTranspiler.render(stmt, builder, environment);
      }
    }

    renderFirstLineAsComment(statement, builder);
    renderSourceMappingComment(statement, builder);
    statement.accept(new SourceTransformer());
  }

  private static void renderSourceMappingComment(Statement statement, SourceBuilder builder) {
    SourcePosition sourcePosition = statement.getSourcePosition();
    if (sourcePosition != SourcePosition.NONE) {
      String filePath = sourcePosition.getPackageRelativePath();
      builder.append(
          String.format(
              ";;@ %s:%d:%d",
              filePath,
              // Lines and column are zero based, but DevTools expects lines to be 1-based and
              // columns to be zeor based.
              sourcePosition.getStartFilePosition().getLine() + 1,
              sourcePosition.getStartFilePosition().getColumn()));
      builder.newLine();
    }
  }

  /** Render first line of the source code for {@code statement} as a WASM comment. * */
  private static void renderFirstLineAsComment(Statement s, SourceBuilder builder) {
    if (s instanceof Block) {
      return;
    }
    String[] parts = s.toString().split("\n", 2);
    builder.append(";; ");
    builder.append(parts[0]);
    builder.newLine();
  }
}
