/*
 * Copyright 2021 Google Inc.
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
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DoWhileStatement;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.LabelReference;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.LoopStatement;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Normalization pass which wraps non-loop labeled statements with "do {...} while (false)".
 *
 * <p>Languages like Kotlin only support break statements that target loops, so in order to support
 * the general Java semantics where any labeled statement can be target of a labeled break, this
 * pass adds a trivial loop enclosing the previously labeled statement.
 */
public class NormalizeLabeledStatements extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          private final Set<Label> seenLabels = new HashSet<>();

          @Override
          public LabelReference rewriteLabelReference(LabelReference labelReference) {
            seenLabels.add(labelReference.getTarget());
            return labelReference;
          }

          @Override
          public Node rewriteLabeledStatement(LabeledStatement labeledStatement) {
            Statement statement = labeledStatement.getStatement();
            if (!seenLabels.contains(labeledStatement.getLabel())) {
              // Remove labels that are not referenced.
              return statement;
            }

            if (statement instanceof LoopStatement) {
              // Don't need any rewriting, preserve the original code.
              return labeledStatement;
            }

            // Introduce a `do { ... } while(false)` since labels that are targets of break need to
            // to be on loop statements.
            return LabeledStatement.Builder.from(labeledStatement)
                .setStatement(
                    DoWhileStatement.newBuilder()
                        .setSourcePosition(statement.getSourcePosition())
                        .setConditionExpression(BooleanLiteral.get(false))
                        .setBodyStatements(statement)
                        .build())
                .build();
          }
        });
  }
}
