/*
 * Copyright 2024 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.passes;


import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BreakOrContinueStatement;
import com.google.j2cl.transpiler.ast.BreakStatement;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ContinueStatement;
import com.google.j2cl.transpiler.ast.DoWhileStatement;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.LabelReference;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.LoopStatement;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.YieldStatement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Remove unnecessary labels that could have been created by J2KT or Kotlin frontend. */
public class RemoveUnnecessaryLabels extends NormalizationPass {

  private final boolean onlyLoopsAreBreakable;

  public RemoveUnnecessaryLabels() {
    this(false);
  }

  public RemoveUnnecessaryLabels(boolean onlyLoopsAreBreakable) {
    this.onlyLoopsAreBreakable = onlyLoopsAreBreakable;
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    removeDoWhileFalseLoops(compilationUnit);
    pushLabelsInward(compilationUnit);
    collapseNestedLabels(compilationUnit);
    removeLabelOnImplicitBreakOrContinueStatement(compilationUnit);
    removeUnreferencedLabels(compilationUnit);
  }

  /**
   * Remove loops of the form `do ... while(false)` since those are commonly inserted by
   * transformation of kotlin code.
   */
  private void removeDoWhileFalseLoops(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          // Collect the labels associated with `do {...} while (false)` loops to convert any
          // continue statements targetting the loop into break statements since in this situation
          // they are equivalent and the label might end up attached to a non-looping statement.
          private final Set<Label> labelsToConvert = new HashSet<>();

          @Override
          public boolean shouldProcessDoWhileStatement(DoWhileStatement doWhileStatement) {
            if (doWhileStatement.getConditionExpression().isBooleanFalse()) {
              addEnclosingLabel();
            }
            return true;
          }

          @Override
          public Node rewriteContinueStatement(ContinueStatement continueStatement) {
            if (!labelsToConvert.contains(continueStatement.getLabelReference().getTarget())) {
              return continueStatement;
            }
            return BreakStatement.Builder.from(continueStatement).build();
          }

          @Override
          public Node rewriteDoWhileStatement(DoWhileStatement doWhileStatement) {
            Statement body = doWhileStatement.getBody();
            if (doWhileStatement.getConditionExpression().isBooleanFalse()
                && (!(getParent() instanceof LabeledStatement) || canBeLabeled(body))) {
              return body;
            }
            return doWhileStatement;
          }

          private void addEnclosingLabel() {
            // Only loops that that are targeted by breaks or continues are labeled.
            if (getParent() instanceof LabeledStatement labeledStatement) {
              labelsToConvert.add(labeledStatement.getLabel());
            }
          }
        });
  }

  /**
   * Move labels applied on blocks to the last statement of the block if there is no break targeting
   * the label in the other statements of the block.
   */
  private void pushLabelsInward(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteLabeledStatement(LabeledStatement labeledStatement) {
            if (!(labeledStatement.getStatement() instanceof Block labeledBlock)) {
              return labeledStatement;
            }

            if (labeledBlock.getStatements().isEmpty()) {
              return labeledBlock;
            }
            Statement lastStatement = Iterables.getLast(labeledBlock.getStatements());
            if (!canBeLabeled(lastStatement)) {
              return labeledStatement;
            }

            List<Statement> allStatementsButLast =
                labeledBlock.getStatements().subList(0, labeledBlock.getStatements().size() - 1);
            if (allStatementsButLast.stream()
                .anyMatch(s -> hasReferencesToLabel(s, labeledStatement.getLabel()))) {
              return labeledStatement;
            }

            return Block.newBuilder()
                .addStatements(allStatementsButLast)
                .addStatement(
                    LabeledStatement.Builder.from(labeledStatement)
                        .setStatement(lastStatement)
                        .build())
                .build();
          }

          private boolean hasReferencesToLabel(Statement statement, Label label) {
            boolean[] breakSeen = {false};
            statement.accept(
                new AbstractVisitor() {
                  @Override
                  public void exitLabelReference(LabelReference labelReference) {
                    if (labelReference.getTarget() == label) {
                      breakSeen[0] = true;
                    }
                  }
                });
            return breakSeen[0];
          }
        });
  }

  /**
   * Collapse nested LabeledStatements and keep the innermost label. <code>
   *   FOO: BAR: BAZ: while(...) {
   *     ...
   *     break FOO;
   *     ...
   *     break BAR;
   *   }
   * </code> will be transformed to: <code>
   *   BAZ: while(...) {
   *     ...
   *     break BAZ;
   *     ...
   *     break BAZ;
   *   }
   * </code>
   */
  private static void collapseNestedLabels(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          private final Map<Label, Label> labelReplacementMap = new HashMap<>();

          @Override
          public boolean shouldProcessLabeledStatement(LabeledStatement labeledStatement) {
            if (!(labeledStatement.getStatement() instanceof LabeledStatement)) {
              return true;
            }
            labelReplacementMap.put(
                labeledStatement.getLabel(), getInnermostLabel(labeledStatement));
            return true;
          }

          private Label getInnermostLabel(LabeledStatement labeledStatement) {
            if (labeledStatement.getStatement() instanceof LabeledStatement innerStatement) {
              return getInnermostLabel(innerStatement);
            }
            return labeledStatement.getLabel();
          }

          @Override
          public Node rewriteLabeledStatement(LabeledStatement labeledStatement) {
            Label replacementLabel = labelReplacementMap.remove(labeledStatement.getLabel());

            if (replacementLabel == null) {
              return labeledStatement;
            }

            return labeledStatement.getStatement();
          }

          @Override
          public Node rewriteBreakOrContinueStatement(
              BreakOrContinueStatement breakOrContinueStatement) {
            if (breakOrContinueStatement.getLabelReference() == null) {
              return breakOrContinueStatement;
            }

            Label replacementLabel =
                labelReplacementMap.get(breakOrContinueStatement.getLabelReference().getTarget());
            if (replacementLabel == null) {
              return breakOrContinueStatement;
            }

            return breakOrContinueStatement.toBuilder()
                .setLabelReference(replacementLabel.createReference())
                .build();
          }

          @Override
          public Node rewriteYieldStatement(YieldStatement yieldStatement) {
            if (yieldStatement.getLabelReference() == null) {
              return yieldStatement;
            }

            Label replacementLabel =
                labelReplacementMap.get(yieldStatement.getLabelReference().getTarget());
            if (replacementLabel == null) {
              return yieldStatement;
            }

            return YieldStatement.Builder.from(yieldStatement)
                .setLabelReference(replacementLabel.createReference())
                .build();
          }
        });
  }

  private static void removeLabelOnImplicitBreakOrContinueStatement(
      CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new LabelAwareRewriter() {
          @Override
          public Node rewriteBreakOrContinueStatement(
              BreakOrContinueStatement breakOrContinueStatement) {
            if (isSameAsImplicitLabel(breakOrContinueStatement)) {
              return breakOrContinueStatement.toBuilder().setLabelReference(null).build();
            }
            return breakOrContinueStatement;
          }

          private boolean isSameAsImplicitLabel(BreakOrContinueStatement breakOrContinueStatement) {
            return getTargetLabel(breakOrContinueStatement)
                == breakOrContinueStatement.getLabelReference().getTarget();
          }
        });
  }

  private static void removeUnreferencedLabels(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          private final Set<Label> labelsSeen = new HashSet<>();

          @Override
          public Node rewriteLabelReference(LabelReference labelReference) {
            labelsSeen.add(labelReference.getTarget());
            return labelReference;
          }

          @Override
          public Node rewriteLabeledStatement(LabeledStatement labeledStatement) {
            if (labelsSeen.contains(labeledStatement.getLabel())) {
              return labeledStatement;
            }
            return labeledStatement.getStatement();
          }
        });
  }

  private boolean canBeLabeled(Statement statement) {
    return !onlyLoopsAreBreakable || statement instanceof LoopStatement;
  }
}
