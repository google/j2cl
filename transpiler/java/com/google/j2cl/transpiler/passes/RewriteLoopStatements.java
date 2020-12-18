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

import static java.util.stream.Collectors.toList;

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
 * Rewrites all LoopStatements into a normalized while loop of the form {@code while(true) body}.
 *
 * <p>Two explicit labels will be assigned to each loop construct:
 *
 * <ul>
 *   <li>a label that will only be targeted by break statement, called the break label,
 *   <li>a label that will only be targeted by continue statement, called the continue label.
 * </ul>
 *
 * <p>After this transformation, "break" labels will always directly enclose a looping construct,
 * whereas "continue" labels will enclose the body of the loop.
 *
 * <p>This transformation will also rewrite all break and continue statements to target the right
 * label. Continue statements will be rewritten as break statements as they are now targeting a
 * labeled statement that is not a loop.
 *
 * <p>This transformation makes it trivial to map looping constructs in the wasm backend while still
 * being a source transformation that preserves Java semantics.
 */
public class RewriteLoopStatements extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    assignLabelsToLoops(compilationUnit);
    normalizeBreakAndContinue(compilationUnit);
    normalizeLoops(compilationUnit);
  }

  /** Assigns break and continue labels to each loop. */
  private void assignLabelsToLoops(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new LabelAwareRewriter() {
          @Override
          public Node rewriteLoopStatement(LoopStatement loopStatement) {

            // Add a continue label to the body:
            // while(x) body  =>  while(x) CONTINUE_LABEL:body
            // do body while (x)  =>  do CONTINUE_LABEL:body while(x)
            // for(x;y;z) body  =>  for(x;y;z) CONTINUE_LABEL:body
            Label continueLabel = Label.newBuilder().setName("LOOP_CONTINUE").build();
            Statement rewrittenLoopStatement =
                LoopStatement.Builder.from(loopStatement)
                    .setBody(loopStatement.getBody().encloseWithLabel(continueLabel))
                    .build();

            if (getEnclosingLabel() == null) {
              // Add a break label if it didn't have one.
              rewrittenLoopStatement =
                  rewrittenLoopStatement.encloseWithLabel(
                      Label.newBuilder().setName("LOOP_BREAK").build());
            }
            return rewrittenLoopStatement;
          }
        });
  }

  /**
   * Rewrite continue statements as break statements and ensure all existing continue and break
   * statements target the right label.
   */
  private void normalizeBreakAndContinue(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new LabelAwareRewriter() {
          Deque<Label> enclosingBreakLabels = new ArrayDeque<>();
          Map<Label, Label> continueLabelsByBreakLabel = new HashMap<>();

          @Override
          public boolean shouldProcessLoopStatement(LoopStatement loopStatement) {
            Label breakLabel = getEnclosingLabel();
            Label continueLabel = ((LabeledStatement) loopStatement.getBody()).getLabel();
            enclosingBreakLabels.push(breakLabel);
            continueLabelsByBreakLabel.put(breakLabel, continueLabel);
            return true;
          }

          @Override
          public Node rewriteLoopStatement(LoopStatement loopStatement) {
            enclosingBreakLabels.pop();
            return loopStatement;
          }

          @Override
          public boolean shouldProcessSwitchStatement(SwitchStatement switchStatement) {
            // TODO(dramaix): remove when SwitchStatement is implemented
            return false;
          }

          @Override
          public Node rewriteContinueStatement(ContinueStatement continueStatement) {
            Label originalTargetLabel =
                continueStatement.getLabelReference() != null
                    ? continueStatement.getLabelReference().getTarget()
                    : enclosingBreakLabels.peek();
            Label continueLabel = continueLabelsByBreakLabel.get(originalTargetLabel);

            return BreakStatement.newBuilder()
                .setLabelReference(continueLabel.createReference())
                .setSourcePosition(continueStatement.getSourcePosition())
                .build();
          }

          @Override
          public Node rewriteBreakStatement(BreakStatement breakStatement) {
            if (breakStatement.getLabelReference() == null) {
              return BreakStatement.Builder.from(breakStatement)
                  .setLabelReference(enclosingBreakLabels.peek().createReference())
                  .build();
            }
            // Labeled break statements remain unchanged.
            return breakStatement;
          }
        });
  }

  /** Rewrite every loop as while(true) loop. */
  private void normalizeLoops(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new LabelAwareRewriter() {

          /**
           * Rewrite {@link WhileStatement} as follows:
           *
           * <pre>{@code
           * while(cdt) {   =>  while(true){
           *   body               if (!cdt) break;
           * }                    body;
           *                    }
           * }</pre>
           */
          @Override
          public Node rewriteWhileStatement(WhileStatement whileStatement) {
            return createWhileTrueLoop(
                whileStatement.getSourcePosition(),
                createConditionalBreakStatement(
                    whileStatement.getSourcePosition(),
                    whileStatement.getConditionExpression(),
                    getEnclosingLabel().createReference()),
                whileStatement.getBody());
          }

          /**
           * Rewrite {@link DoWhileStatement} as follows:
           *
           * <pre>{@code
           * do {            =>  while(true){
           *   body                body;
           * } while(cdt);         if (!cdt) break;
           *                     }
           * }</pre>
           */
          @Override
          public Node rewriteDoWhileStatement(DoWhileStatement doWhileStatement) {
            return createWhileTrueLoop(
                doWhileStatement.getSourcePosition(),
                doWhileStatement.getBody(),
                createConditionalBreakStatement(
                    doWhileStatement.getSourcePosition(),
                    doWhileStatement.getConditionExpression(),
                    getEnclosingLabel().createReference()));
          }

          /**
           * Rewrite {@link ForStatement} as follows:
           *
           * <pre>{@code
           * for(init;cdt;updates) {  => {init;
           *   body                       while(true){
           * }                            if (!cdt) break;
           *                                body;
           *                                updates;
           *                             }
           * }</pre>
           */
          @Override
          public Node rewriteForStatement(ForStatement forStatement) {
            return Block.newBuilder()
                .setStatements(
                    forStatement.getInitializers().stream()
                        .map(e -> e.makeStatement(forStatement.getSourcePosition()))
                        .collect(toList()))
                .addStatement(
                    createWhileTrueLoop(
                        forStatement.getSourcePosition(),
                        createConditionalBreakStatement(
                            forStatement.getSourcePosition(),
                            forStatement.getConditionExpression(),
                            getEnclosingLabel().createReference()),
                        forStatement.getBody(),
                        Block.newBuilder()
                            .setStatements(
                                forStatement.getUpdates().stream()
                                    .map(e -> e.makeStatement(forStatement.getSourcePosition()))
                                    .collect(Collectors.toList()))
                            .build()))
                .build();
          }

          private WhileStatement createWhileTrueLoop(
              SourcePosition sourcePosition, Statement... bodyStatements) {
            return WhileStatement.newBuilder()
                .setSourcePosition(sourcePosition)
                .setConditionExpression(BooleanLiteral.get(true))
                .setBody(
                    Block.newBuilder()
                        .setStatements(bodyStatements)
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
        });
  }

  private abstract static class LabelAwareRewriter extends AbstractRewriter {
    public Label getEnclosingLabel() {
      return getParent() instanceof LabeledStatement
          ? ((LabeledStatement) getParent()).getLabel()
          : null;
    }
  }
}
