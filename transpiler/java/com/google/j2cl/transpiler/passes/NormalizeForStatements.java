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

import com.google.common.base.MoreObjects;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ForStatement;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;

/** Normalize ForStatement by moving the init part out of for loops. */
public class NormalizeForStatements extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // Rewrite all labeled for loop first:
    // LABEL:
    // for(init; cdt; updates) {
    //   body
    // }
    //
    // to
    //
    // init;
    // LABEL:
    // for(;cdt; updates) {
    //   body
    // }
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteLabeledStatement(LabeledStatement labeledStatement) {
            if (!(labeledStatement.getStatement() instanceof ForStatement)) {
              return labeledStatement;
            }

            return moveInitializersOutwards(
                (ForStatement) labeledStatement.getStatement(), labeledStatement);
          }
        });

    // rewrite other for loops with initializers
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteForStatement(ForStatement forStatement) {
            return moveInitializersOutwards(forStatement, null);
          }
        });
  }

  private static Statement moveInitializersOutwards(
      ForStatement forStatement, LabeledStatement parentLabel) {
    if (forStatement.getInitializers().isEmpty()) {
      return MoreObjects.firstNonNull(parentLabel, forStatement);
    }

    Statement forStatementWithoutInitializers =
        ForStatement.Builder.from(forStatement).setInitializers().build();

    return Block.newBuilder()
        .setStatements(
            forStatement.getInitializers().stream()
                .map(e -> e.makeStatement(forStatement.getSourcePosition()))
                .collect(toList()))
        .addStatement(
            parentLabel != null
                ? LabeledStatement.Builder.from(parentLabel)
                    .setStatement(forStatementWithoutInitializers)
                    .build()
                : forStatementWithoutInitializers)
        .setSourcePosition(forStatement.getSourcePosition())
        .build();
  }
}
