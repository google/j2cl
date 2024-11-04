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

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.transpiler.ast.BreakOrContinueStatement;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.LoopStatement;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchStatement;

/**
 * Assigns a label to each loop and switch that does not already have one, and makes all breaks and
 * continues explicitly target a label. After this pass is ran all breaks and continues will have an
 * explicit target.
 */
public class NormalizeLabels extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new LabelAwareRewriter() {
          @Override
          protected Statement rewriteLoopStatement(
              LoopStatement loopStatement, Label assignedLabel) {
            return ensureLabeled(loopStatement, assignedLabel);
          }

          @Override
          protected Statement rewriteSwitchStatement(
              SwitchStatement switchStatement, Label assignedLabel) {
            return ensureLabeled(switchStatement, assignedLabel);
          }

          private Statement ensureLabeled(Statement statement, Label label) {
            if (getParent() instanceof LabeledStatement) {
              checkState(((LabeledStatement) getParent()).getLabel() == label);
              return statement;
            }

            // Make sure statement is enclosed with the label (if not already).
            return statement.encloseWithLabel(label);
          }

          @Override
          public Node rewriteBreakOrContinueStatement(
              BreakOrContinueStatement breakOrContinueStatement) {
            if (breakOrContinueStatement.getLabelReference() != null) {
              return breakOrContinueStatement;
            }

            return breakOrContinueStatement.toBuilder()
                .setLabelReference(getTargetLabel(breakOrContinueStatement).createReference())
                .build();
          }
        });
  }
}
