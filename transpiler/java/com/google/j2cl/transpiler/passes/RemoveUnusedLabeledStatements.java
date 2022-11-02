/*
 * Copyright 2022 Google Inc.
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
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DoWhileStatement;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.LabelReference;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Remove unused labeled statements and accompanying "do {} while (false)" statements.
 *
 * <p>This pass assumes that all break and continue statements contains label.
 */
public class RemoveUnusedLabeledStatements extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    Set<Label> usedLabels = new HashSet<>();

    // Collect used labels.
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitLabelReference(LabelReference labelReference) {
            usedLabels.add(labelReference.getTarget());
          }
        });

    // Remove unused labeled statements.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteLabeledStatement(LabeledStatement labeledStatement) {
            Statement statement = labeledStatement.getStatement();
            if (usedLabels.contains(labeledStatement.getLabel())) {
              return labeledStatement;
            }

            // Remove "do { ... } while (false)" block if needed
            if (statement instanceof DoWhileStatement) {
              DoWhileStatement doWhileStatement = (DoWhileStatement) statement;
              Expression conditionExpression = doWhileStatement.getConditionExpression();
              if (conditionExpression instanceof BooleanLiteral) {
                BooleanLiteral booleanLiteral = (BooleanLiteral) conditionExpression;
                if (!booleanLiteral.getValue()) {
                  return doWhileStatement.getBody();
                }
              }
            }

            return statement;
          }
        });
  }
}
