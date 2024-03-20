/*
 * Copyright 2019 Google Inc.
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
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BreakOrContinueStatement;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchCase;
import javax.annotation.Nullable;

/**
 * Removes statements that have no effect.
 *
 * <p>Some of the normalization passes might leave statements that are empty or just a literal, this
 * pass performs the cleanup.
 */
public class RemoveNoopStatements extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          @Nullable
          public Statement rewriteStatement(Statement statement) {
            if (!statement.isNoop()) {
              return statement;
            }

            // Remove the statement if possible otherwise replace with a canonical form.
            return isRemovableFromParent() ? null : Statement.createNoopStatement();
          }

          private boolean isRemovableFromParent() {
            // Statements can only be removed if they are part of a list in the parent
            return getParent() instanceof Block || getParent() instanceof SwitchCase;
          }

          // TODO(b/330169941): The removal of trivially useless labels is necessary due to this.
          @Override
          public Statement rewriteLabeledStatement(LabeledStatement labeledStatement) {
            return rewriteStatement(removeLabelIfPossible(labeledStatement));
          }

          private Statement removeLabelIfPossible(LabeledStatement labeledStatement) {
            Statement innerStatement = labeledStatement.getStatement();
            if (innerStatement instanceof BreakOrContinueStatement) {
              // Remove the label in code like `L1: break L2` since that chokes jscompiler.
              BreakOrContinueStatement breakOrContinueStatement =
                  (BreakOrContinueStatement) innerStatement;
              if (!breakOrContinueStatement.targetsLabel(labeledStatement.getLabel())) {
                return breakOrContinueStatement;
              }
            }
            return labeledStatement;
          }
        });
  }
}
