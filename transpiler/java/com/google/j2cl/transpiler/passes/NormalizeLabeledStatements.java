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
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DoWhileStatement;
import com.google.j2cl.transpiler.ast.ForEachStatement;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.LoopStatement;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;

/** Normalization pass which wraps non-loop labeled statements with "do {...} while (false)". */
public class NormalizeLabeledStatements extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteLabeledStatement(LabeledStatement labeledStatement) {
            Statement statement = labeledStatement.getStatement();
            return statement instanceof LoopStatement || statement instanceof ForEachStatement
                ? labeledStatement
                : LabeledStatement.Builder.from(labeledStatement)
                    .setStatement(
                        DoWhileStatement.newBuilder()
                            .setSourcePosition(statement.getSourcePosition())
                            .setConditionExpression(BooleanLiteral.get(false))
                            .setBody(
                                statement instanceof Block
                                    ? statement
                                    : Block.newBuilder()
                                        .setSourcePosition(labeledStatement.getSourcePosition())
                                        .setStatements(statement)
                                        .build())
                            .build())
                    .build();
          }
        });
  }
}
