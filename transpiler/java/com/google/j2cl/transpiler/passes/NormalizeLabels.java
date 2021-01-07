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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BreakStatement;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ContinueStatement;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.LoopStatement;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Assigns a label to each loop and switch that does not already have one, and makes all breaks and
 * continues explicitly target a label. After this pass is ran all breaks and continues will have an
 * explicit target.
 *
 * <p>TODO(rluble): This pass does not handle switch statements yet.
 */
public class NormalizeLabels extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          Deque<Label> enclosingLoopLabels = new ArrayDeque<>();

          @Override
          public boolean shouldProcessLoopStatement(LoopStatement loopStatement) {
            Label enclosingLabel =
                getParent() instanceof LabeledStatement
                    ? ((LabeledStatement) getParent()).getLabel()
                    // Add a label for break and continues if the loop didn't have one.
                    : Label.newBuilder().setName("LOOP").build();

            enclosingLoopLabels.push(enclosingLabel);
            return true;
          }

          @Override
          public Statement rewriteLoopStatement(LoopStatement loopStatement) {
            Label loopLabel = enclosingLoopLabels.pop();
            if (getParent() instanceof LabeledStatement) {
              return loopStatement;
            }
            // Make sure loop is enclosed with the label if not already.
            return loopStatement.encloseWithLabel(loopLabel);
          }

          @Override
          public boolean shouldProcessSwitchStatement(SwitchStatement switchStatement) {
            // TODO(dramaix): remove when SwitchStatement is implemented
            return false;
          }

          @Override
          public Node rewriteContinueStatement(ContinueStatement continueStatement) {
            if (continueStatement.getLabelReference() != null) {
              return continueStatement;
            }

            return ContinueStatement.newBuilder()
                .setLabelReference(enclosingLoopLabels.peek().createReference())
                .setSourcePosition(continueStatement.getSourcePosition())
                .build();
          }

          @Override
          public Node rewriteBreakStatement(BreakStatement breakStatement) {
            if (breakStatement.getLabelReference() != null) {
              return breakStatement;
            }
            return BreakStatement.Builder.from(breakStatement)
                .setLabelReference(enclosingLoopLabels.peek().createReference())
                .build();
          }
        });
  }
}
