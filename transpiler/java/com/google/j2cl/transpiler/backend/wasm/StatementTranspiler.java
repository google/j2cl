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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.not;
import static java.util.Arrays.stream;

import com.google.common.collect.Iterables;
import com.google.common.math.Stats;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.BreakStatement;
import com.google.j2cl.transpiler.ast.CatchClause;
import com.google.j2cl.transpiler.ast.ContinueStatement;
import com.google.j2cl.transpiler.ast.DoWhileStatement;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.ForStatement;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.LoopStatement;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.SynchronizedStatement;
import com.google.j2cl.transpiler.ast.ThrowStatement;
import com.google.j2cl.transpiler.ast.TryStatement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.WhileStatement;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.util.Arrays;
import java.util.List;

/** Transforms Statements into Wasm code. */
final class StatementTranspiler {

  public static void render(
      Statement statement,
      final SourceBuilder builder,
      final WasmGenerationEnvironment environment) {

    class SourceTransformer extends AbstractVisitor {
      @Override
      public boolean enterStatement(Statement statement) {
        throw new IllegalStateException("Unhandled statement " + statement);
      }

      @Override
      public boolean enterBlock(Block block) {
        builder.newLine();
        builder.openParens("block");
        renderStatements(block.getStatements());
        builder.closeParens();
        return false;
      }

      private void renderStatements(List<Statement> statements) {
        statements.forEach(this::render);
      }

      @Override
      public boolean enterBreakStatement(BreakStatement breakStatement) {
        builder.newLine();
        builder.append(
            "(br " + getBreakLabelName(breakStatement.getLabelReference().getTarget()) + ")");
        return false;
      }

      @Override
      public boolean enterContinueStatement(ContinueStatement continueStatement) {
        builder.newLine();
        builder.append(
            "(br " + getContinueLabelName(continueStatement.getLabelReference().getTarget()) + ")");
        return false;
      }

      @Override
      public boolean enterExpressionStatement(ExpressionStatement expressionStatement) {
        Expression expression = expressionStatement.getExpression();
        builder.newLine();
        builder.emitWithMapping(
            expressionStatement.getSourcePosition(),
            () -> ExpressionTranspiler.renderWithUnusedResult(expression, builder, environment));

        return false;
      }

      @Override
      public boolean enterIfStatement(IfStatement ifStatement) {
        builder.newLine();
        builder.openParens("if ");
        builder.emitWithMapping(
            ifStatement.getSourcePosition(),
            () -> renderExpression(ifStatement.getConditionExpression()));
        builder.newLine();
        builder.openParens("then");
        render(ifStatement.getThenStatement());
        builder.closeParens();
        if (ifStatement.getElseStatement() != null) {
          builder.openParens("else");
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
        builder.newLine();
        builder.openParens("block " + label);
        render(labeledStatement.getStatement());
        builder.closeParens();
        return false;
      }

      @Override
      public boolean enterReturnStatement(ReturnStatement returnStatement) {
        builder.emitWithMapping(
            returnStatement.getSourcePosition(),
            () -> {
              builder.newLine();
              builder.append("(return ");
              if (returnStatement.getExpression() != null) {
                ExpressionTranspiler.render(returnStatement.getExpression(), builder, environment);
              }
              builder.append(")");
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
        if (isPrimitiveOrJsEnum(switchStatement.getSwitchExpression().getTypeDescriptor())) {
          Stats stats =
              Stats.of(
                  switchStatement.getCases().stream()
                      .filter(not(SwitchCase::isDefault))
                      .mapToInt(StatementTranspiler::getSwitchCaseAsIntValue));
          if (isDense(stats)) {
            renderDenseSwitchDispatchTable(switchStatement, stats);
            return;
          }
        }
        renderNonDenseSwitchDispatchTable(switchStatement);
      }

      private boolean isPrimitiveOrJsEnum(TypeDescriptor typeDescriptor) {
        return typeDescriptor.isPrimitive() || AstUtils.isPrimitiveNonNativeJsEnum(typeDescriptor);
      }

      /**
       * The minimum ratio between used case slots and total case slots to consider a switch dense.
       * Emitting br_table with a quarter of the slots empty is still shorter than emitting a nest
       * of br_ifs. This could be tuned to allow sparser or denser tables.
       */
      private static final double MINIMUM_CASE_DENSITY = 0.25;

      private boolean isDense(Stats caseStats) {
        return caseStats.count() > 0
            && caseStats.count() / (caseStats.max() - caseStats.min()) > MINIMUM_CASE_DENSITY;
      }

      private void renderDenseSwitchDispatchTable(
          SwitchStatement switchStatement, Stats caseStats) {
        // br_table allows to specify a table of jump target locations that is indexed by a
        // zero-based i32 value.
        // e.g.:
        // (br_table  $l1 $l2 $l3 (expr)) would jump to $l1 if expr == 0, to $l2 if expr == 1, and
        // to $l3 otherwise.
        //
        // In real life, switch cases are not necessarily dense nor start from a zero index.
        // In order to efficiently utilize this instruction, we need to shift indices and also
        // handle gaps.
        // e.g.
        //
        //   int i = ...;
        //   switch (i) {
        //     case 2:
        //       ......
        //     case 7:
        //       ......
        //     case 5:
        //       ......
        //     default:
        //       ......
        //     }
        //
        // is emitted as
        //
        // (block $LDefault
        //   (block $L5
        //     (block $L7
        //       (block $L2
        //         (br_table
        //           $L2
        //           $LDefault   // hole for value 3
        //           $LDefault   // hole for value 4
        //           $L5
        //           $LDefault   // hole for value 5
        //           $L7
        //           $LDefault   // values > 5 and < 2
        //           (i32.sub (local.get $i - (i32.const 2))))
        //         )
        //       )
        //       ; code in case 2:
        //     )
        //     ; code in case 7:
        //   )
        //   ; code in case 5:
        // )
        // ; code in default case
        //
        // Here the first label will correspond to the minimum case value ($L2), $LDefault would be
        // the block that handles the default case. Also note that the expression needs to be
        // adjusted to handle the offset, which is the minimum case value (i.e. expr - 2).
        List<SwitchCase> switchCases = switchStatement.getCases();

        // The number of labels that are needed are one for each integer from the minimum value to
        // the maximum value (inclusive) plus an extra slot for the default.
        int[] slots = new int[(int) (caseStats.max() - caseStats.min() + 2)];

        int defaultCasePosition = switchStatement.getDefaultCasePosition();
        if (defaultCasePosition == -1) {
          // If there is no default using the number of cases will jump out of the switch all
          // together.
          defaultCasePosition = switchCases.size();
        }

        Arrays.fill(slots, defaultCasePosition);

        int offset = (int) caseStats.min();
        for (int casePosition = 0; casePosition < switchCases.size(); casePosition++) {
          SwitchCase switchCase = switchCases.get(casePosition);
          if (switchCase.isDefault()) {
            continue;
          }
          slots[getSwitchCaseAsIntValue(switchCase) - offset] = casePosition;
        }

        builder.newLine();
        builder.openParens("block ;; evaluate expression and jump");

        builder.newLine();
        builder.append("(br_table ");
        stream(slots).forEach(slot -> builder.append(slot + " "));
        emitBranchIndexExpression(switchStatement.getSwitchExpression(), offset);
        builder.append(")");
        builder.closeParens();
      }

      private void emitBranchIndexExpression(Expression expression, int offset) {
        if (offset == 0) {
          renderExpression(expression);
        } else {
          builder.append("(i32.sub ");
          renderExpression(expression);
          builder.append(" (i32.const " + offset + "))");
        }
      }

      private void renderNonDenseSwitchDispatchTable(SwitchStatement switchStatement) {
        // Evaluate the switch expression and jump to the right case.
        builder.newLine();
        builder.openParens("block ;; evaluate expression and jump");

        for (int casePosition = 0;
            casePosition < switchStatement.getCases().size();
            casePosition++) {
          // Emit conditions for each case.
          SwitchCase switchCase = switchStatement.getCases().get(casePosition);
          if (switchCase.isDefault()) {
            // Skip the default case, since all the other conditions need to be evaluated before,
            // and the default case is handled by an unconditional branch after all other conditions
            // are checked.
            continue;
          }
          // If the condition for this case is met, jump to the start of the case, i.e. jump out
          // of all of the previous enclosing blocks.
          Expression condition =
              createCaseCondition(
                  switchCase.getCaseExpression(), switchStatement.getSwitchExpression());
          renderConditionalBranch(switchStatement.getSourcePosition(), condition, casePosition);
        }

        // When no other condition was met, jump to the default case if exists.
        int defaultCasePosition = switchStatement.getDefaultCasePosition();
        renderUnconditionalBranch(
            defaultCasePosition != -1 ? defaultCasePosition : switchStatement.getCases().size());
        builder.closeParens();
      }

      /** Creates the condition to compare the switch expression with the case expression. */
      private Expression createCaseCondition(
          Expression switchCaseExpression, Expression switchExpression) {
        if (TypeDescriptors.isJavaLangString(switchCaseExpression.getTypeDescriptor())) {
          // Strings are compared using equals.
          return RuntimeMethods.createStringEqualsMethodCall(
              switchCaseExpression, switchExpression);
        }

        checkState(switchCaseExpression.getTypeDescriptor().isPrimitive());
        return switchExpression.infixEquals(switchCaseExpression);
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
              renderExpression(throwStatement.getExpression());
              // Since throw in JS invisible, adding unreachable keeps the Wasm invariants.
              builder.newLine();
              builder.append("(unreachable)");
            });
        return false;
      }

      @Override
      public boolean enterTryStatement(TryStatement tryStatement) {
        builder.newLine();
        CatchClause catchClause = Iterables.getOnlyElement(tryStatement.getCatchClauses(), null);
        if (catchClause != null) {
          builder.append("(try (do");
          builder.indent();
          render(tryStatement.getBody());
          builder.unindent();
          builder.newLine();
          builder.append(") (catch $exception.event");
          builder.indent();
          builder.newLine();
          builder.append(
              String.format(
                  "(local.set %s (pop externref))",
                  environment.getDeclarationName(catchClause.getExceptionVariable())));
          render(catchClause.getBody());
          builder.unindent();
          builder.newLine();
          builder.append("))");
        } else {
          render(tryStatement.getBody());
        }
        checkState(tryStatement.getFinallyBlock() == null);
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
          // Removing this condition in these cases brings the Wasm verifier to be inline with
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
        builder.closeParens();
        builder.closeParens();
      }

      void renderLabeledStatement(String label, Statement statement) {
        builder.newLine();
        builder.openParens("block " + label);
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

    if (!(statement instanceof Block)) {
      renderSourceMappingComment(statement.getSourcePosition(), builder);
    }
    statement.accept(new SourceTransformer());
  }

  public static void renderSourceMappingComment(
      SourcePosition sourcePosition, SourceBuilder builder) {
    if (sourcePosition != SourcePosition.NONE) {
      builder.newLine();
      builder.append(
          String.format(
              ";;@ %s:%d:%d",
              sourcePosition.getPackageRelativePath(),
              // Lines and column are zero based, but DevTools expects lines to be 1-based and
              // columns to be zero based.
              sourcePosition.getStartFilePosition().getLine() + 1,
              sourcePosition.getStartFilePosition().getColumn()));
    }
  }

  private static int getSwitchCaseAsIntValue(SwitchCase switchCase) {
    NumberLiteral caseExpression = (NumberLiteral) switchCase.getCaseExpression();
    return caseExpression.getValue().intValue();
  }

  private StatementTranspiler() {}
}
