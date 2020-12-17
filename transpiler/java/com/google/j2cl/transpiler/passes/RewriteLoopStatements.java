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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.BreakStatement;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ContinueStatement;
import com.google.j2cl.transpiler.ast.DoWhileStatement;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ForStatement;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.LabelReference;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.LoopStatement;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.WhileStatement;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Rewrites all LoopStatement in a form of {@code While(true)} loop.
 *
 * <p>Each loop construct will be associated to three labels:
 *
 * <ul>
 *   <li>a label that will only be targeted by break statement, called the break label,
 *   <li>a label that will only be targeted by continue statement, called the continue label.
 *   <li>a label that will be used to re-enter the loop, called the loop label.
 * </ul>
 *
 * <p>After this transformation, "loop" labels will always directly enclose a looping construct,
 * whereas "break" and "continue" labels will never enclose a looping construct.
 *
 * <p>This transformation will also rewrite all break and continue statements to target the right
 * label. Continue statements will be rewritten as break statements as they are now targeting a
 * labeled statement that is not a loop.
 *
 * <p>This transformation makes it trivial to map looping constructs in the wasm backend while still
 * being a source transformation that preserves Java semantics.
 *
 * <p>With this transformation, the wasm backend, can render "break" and "continue" labeled
 * statements as a "block" instructions with that label; whereas "loop" labeled statements can be
 * rendered as "loop" instructions. Both break and continue statements can be rendered as "branch"
 * instructions; and looping is achieved by just "branching" to a "loop" construct.
 */
public class RewriteLoopStatements extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    Map<Statement, LoopLabels> labelsByLoopStatement = new HashMap<>();
    Map<Label, Label> breakLabelByContinueLabel = new HashMap<>();

    assignLabelsToLoops(compilationUnit, labelsByLoopStatement, breakLabelByContinueLabel);
    normalizeBreakAndContinue(compilationUnit, labelsByLoopStatement, breakLabelByContinueLabel);
    normalizeLoops(compilationUnit, labelsByLoopStatement);
  }

  /** Assigns a break, continue and loop label to each loop. */
  private void assignLabelsToLoops(
      CompilationUnit compilationUnit,
      Map<Statement, LoopLabels> labelsByLoopStatement,
      Map<Label, Label> breakLabelByContinueLabel) {
    compilationUnit.accept(
        new AbstractRewriter() {
          Deque<LabeledStatement> enclosingLabeledStatements = new ArrayDeque<>();

          @Override
          public boolean shouldProcessLabeledStatement(LabeledStatement labeledStatement) {
            enclosingLabeledStatements.push(labeledStatement);
            return true;
          }

          @Override
          public Node rewriteLabeledStatement(LabeledStatement labeledStatement) {
            enclosingLabeledStatements.pop();
            Statement statement = labeledStatement.getStatement();

            if (statement instanceof LoopStatement) {
              return addLabels(
                  labeledStatement.getSourcePosition(),
                  (LoopStatement) statement,
                  labeledStatement.getLabel());
            }

            return labeledStatement;
          }

          @Override
          public Node rewriteLoopStatement(LoopStatement loopStatement) {
            if (hasLabel(loopStatement)) {
              // This statement will be handled by the via its enclosing LabeledStatement.
              return loopStatement;
            }
            return addLabels(
                loopStatement.getSourcePosition(),
                loopStatement,
                Label.newBuilder().setName("$CONTINUE").build());
          }

          private boolean hasLabel(Statement statement) {
            LabeledStatement lastLabeledStatement = enclosingLabeledStatements.peek();
            return lastLabeledStatement != null && lastLabeledStatement.getStatement() == statement;
          }

          private Node addLabels(
              SourcePosition labelSourcePosition,
              LoopStatement loopStatement,
              Label continueLabel) {
            LoopLabels labels = createLabels(continueLabel);

            // while(x) body  =>  BREAK_LABEL: LOOP_LABEL: while(x) CONTINUE_LABEL:body
            // do body while (x)  =>  BREAK_LABEL: LOOP_LABEL: do CONTINUE_LABEL:body while(x)
            // for(x;y;z) body  =>  BREAK_LABEL: LOOP_LABEL: for(x;y;z) CONTINUE_LABEL:body
            LoopStatement loopStatementWithContinueLabel =
                LoopStatement.Builder.from(loopStatement)
                    .setBody(
                        LabeledStatement.newBuilder()
                            .setLabel(labels.continueLabel)
                            .setStatement(loopStatement.getBody())
                            .setSourcePosition(labelSourcePosition)
                            .build())
                    .build();

            LabeledStatement enclosingLabeledStatement =
                LabeledStatement.newBuilder()
                    .setLabel(labels.breakLabel)
                    .setStatement(
                        LabeledStatement.newBuilder()
                            .setLabel(labels.loopLabel)
                            .setStatement(loopStatementWithContinueLabel)
                            .setSourcePosition(loopStatement.getSourcePosition())
                            .build())
                    .setSourcePosition(loopStatement.getSourcePosition())
                    .build();

            // Record labels assigned to this loop for rewriting breaks and continues.
            labelsByLoopStatement.put(loopStatementWithContinueLabel, labels);
            return enclosingLabeledStatement;
          }

          private LoopLabels createLabels(Label continueLabel) {
            checkNotNull(continueLabel);
            Label loopLabel = Label.newBuilder().setName("$LOOP").build();
            Label breakLabel = Label.newBuilder().setName("$BREAK").build();

            breakLabelByContinueLabel.put(continueLabel, breakLabel);

            return new LoopLabels(breakLabel, continueLabel, loopLabel);
          }
        });
  }

  /**
   * Rewrite continue statement as break statements and ensure all existing continue and break
   * statements target the right label.
   */
  private void normalizeBreakAndContinue(
      CompilationUnit compilationUnit,
      Map<Statement, LoopLabels> labelsByLoopStatement,
      Map<Label, Label> breakLabelByContinueLabel) {
    compilationUnit.accept(
        new AbstractRewriter() {
          Deque<LoopLabels> enclosingLabels = new ArrayDeque<>();

          @Override
          public boolean shouldProcessLoopStatement(LoopStatement loopStatement) {
            enclosingLabels.push(labelsByLoopStatement.get(loopStatement));
            return true;
          }

          @Override
          public Node rewriteLoopStatement(LoopStatement loopStatement) {
            enclosingLabels.pop();
            return loopStatement;
          }

          @Override
          public boolean shouldProcessSwitchStatement(SwitchStatement switchStatement) {
            // TODO(dramaix): remove when SwitchStatement is implemented
            return false;
          }

          @Override
          public Node rewriteContinueStatement(ContinueStatement continueStatement) {
            Label continueLabel =
                continueStatement.getLabelReference() != null
                    ? continueStatement.getLabelReference().getTarget()
                    : getEnclosingContinueLabel();

            return BreakStatement.newBuilder()
                .setLabelReference(continueLabel.createReference())
                .setSourcePosition(continueStatement.getSourcePosition())
                .build();
          }

          @Override
          public Node rewriteBreakStatement(BreakStatement breakStatement) {
            if (breakStatement.getLabelReference() == null) {
              return BreakStatement.Builder.from(breakStatement)
                  .setLabelReference(getEnclosingBreakLabel().createReference())
                  .build();
            }

            // Change the break statement that targets loops with the corresponding loop break.
            Label originalLabel = breakStatement.getLabelReference().getTarget();
            Label breakLabel = breakLabelByContinueLabel.get(originalLabel);
            if (breakLabel != null) {
              return BreakStatement.Builder.from(breakStatement)
                  .setLabelReference(breakLabel.createReference())
                  .build();
            }

            // Remaining break statements doesn't break a loop construct.
            return breakStatement;
          }

          private Label getEnclosingContinueLabel() {
            return enclosingLabels.peek().continueLabel;
          }

          private Label getEnclosingBreakLabel() {
            return enclosingLabels.peek().breakLabel;
          }
        });
  }

  /** Rewrite every loop as while(true) loop. */
  private void normalizeLoops(
      CompilationUnit compilationUnit, Map<Statement, LoopLabels> labelsByLoopStatement) {
    compilationUnit.accept(
        new AbstractRewriter() {

          /**
           * Rewrite {@link WhileStatement} as While true loop:
           *
           * <pre>{@code
           * while(cdt) {   =>  while(true){
           *   body               if (!cdt) break;
           * }                    body;
           *                      continue;
           *                    }
           * }</pre>
           */
          @Override
          public Node rewriteWhileStatement(WhileStatement whileStatement) {
            return createWhileTrueLoop(
                whileStatement.getSourcePosition(),
                createLoopLabelReference(whileStatement),
                createConditionalBreakStatement(
                    whileStatement.getSourcePosition(),
                    whileStatement.getConditionExpression(),
                    createBreakLabelReference(whileStatement)),
                whileStatement.getBody());
          }

          /**
           * Rewrite {@link DoWhileStatement} as While true loop:
           *
           * <pre>{@code
           * do {            =>  while(true){
           *   body                body;
           * } while(cdt);         if (!cdt) break;
           *                       continue;
           *                     }
           * }</pre>
           */
          @Override
          public Node rewriteDoWhileStatement(DoWhileStatement doWhileStatement) {
            return createWhileTrueLoop(
                doWhileStatement.getSourcePosition(),
                createLoopLabelReference(doWhileStatement),
                doWhileStatement.getBody(),
                createConditionalBreakStatement(
                    doWhileStatement.getSourcePosition(),
                    doWhileStatement.getConditionExpression(),
                    createBreakLabelReference(doWhileStatement)));
          }

          /**
           * Rewrite {@link ForStatement} as While true loop:
           *
           * <pre>{@code
           * for(;cdt;updates) {  =>  while(true){
           *   body                     if (!cdt) break;
           * }                          body;
           *                            updates;
           *                            continue;
           *                          }
           * }</pre>
           */
          @Override
          public Node rewriteForStatement(ForStatement forStatement) {
            return createWhileTrueLoop(
                forStatement.getSourcePosition(),
                createLoopLabelReference(forStatement),
                createConditionalBreakStatement(
                    forStatement.getSourcePosition(),
                    forStatement.getConditionExpression(),
                    createBreakLabelReference(forStatement)),
                forStatement.getBody(),
                Block.newBuilder()
                    .setStatements(
                        forStatement.getUpdates().stream()
                            .map(e -> e.makeStatement(forStatement.getSourcePosition()))
                            .collect(Collectors.toList()))
                    .build());
          }

          private WhileStatement createWhileTrueLoop(
              SourcePosition sourcePosition,
              LabelReference loopLabel,
              Statement... bodyStatements) {
            return WhileStatement.newBuilder()
                .setSourcePosition(sourcePosition)
                .setConditionExpression(BooleanLiteral.get(true))
                .setBody(
                    Block.newBuilder()
                        .setStatements(bodyStatements)
                        .addStatement(
                            ContinueStatement.newBuilder()
                                .setLabelReference(loopLabel)
                                .setSourcePosition(sourcePosition)
                                .build())
                        .setSourcePosition(sourcePosition)
                        .build())
                .build();
          }

          private Statement createConditionalBreakStatement(
              SourcePosition sourcePosition, Expression condition, LabelReference breakLabel) {

            return IfStatement.newBuilder()
                .setConditionExpression(condition.prefixNot())
                .setThenStatement(
                    BreakStatement.newBuilder()
                        .setLabelReference(breakLabel)
                        .setSourcePosition(sourcePosition)
                        .build())
                .setSourcePosition(sourcePosition)
                .build();
          }

          private LabelReference createLoopLabelReference(LoopStatement loopStatement) {
            return labelsByLoopStatement.get(loopStatement).loopLabel.createReference();
          }

          private LabelReference createBreakLabelReference(LoopStatement loopStatement) {
            return labelsByLoopStatement.get(loopStatement).breakLabel.createReference();
          }
        });
  }

  /** Container for labels applied to a loop construct. */
  private static final class LoopLabels {
    private final Label breakLabel;
    private final Label continueLabel;
    private final Label loopLabel;

    private LoopLabels(Label breakLabel, Label continueLabel, Label loopLabel) {
      this.breakLabel = breakLabel;
      this.continueLabel = continueLabel;
      this.loopLabel = loopLabel;
    }
  }
}
