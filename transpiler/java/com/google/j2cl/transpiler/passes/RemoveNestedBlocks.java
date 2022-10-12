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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchCase;
import java.util.List;
import java.util.stream.Stream;

/** Normalization pass which removes nested blocks. */
public class RemoveNestedBlocks extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBlock(Block block) {
            return Block.Builder.from(block).setStatements(flatten(block.getStatements())).build();
          }

          @Override
          public Node rewriteSwitchCase(SwitchCase switchCase) {
            return SwitchCase.Builder.from(switchCase)
                .setStatements(flatten(switchCase.getStatements()))
                .build();
          }
        });
  }

  private static ImmutableList<Statement> flatten(List<Statement> statements) {
    return statements.stream()
        .flatMap(
            statement ->
                statement instanceof Block
                    ? ((Block) statement).getStatements().stream()
                    : Stream.of(statement))
        .collect(toImmutableList());
  }
}
