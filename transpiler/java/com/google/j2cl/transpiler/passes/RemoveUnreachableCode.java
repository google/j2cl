/*
 * Copyright 2023 Google Inc.
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
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.Type;
import java.util.List;

/**
 * Removes provably unreachable code.
 *
 * <p>There are cases where unreachable code will cause the JSCompiler to see the receiver of a
 * property reference as unknown. For example:
 *
 * <pre>
 *   function foo() {
 *     return;
 *     const a = new Bar();
 *     a.buzz; // [JSC_CONFORMANCE_VIOLATION] Violation: Type of property reference is unknown.
 *   }
 * </pre>
 */
public final class RemoveUnreachableCode extends NormalizationPass {
  @Override
  public void applyTo(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBlock(Block block) {
            List<Statement> filteredStatements = filterUnreachableStatements(block.getStatements());
            if (filteredStatements.size() == block.getStatements().size()) {
              return block;
            }
            return Block.Builder.from(block).setStatements(filteredStatements).build();
          }
        });
  }

  private static List<Statement> filterUnreachableStatements(List<Statement> statements) {
    int terminatingStatement = Iterables.indexOf(statements, Statement::terminatesAbruptly);
    if (terminatingStatement == -1) {
      return statements;
    }
    return statements.subList(0, terminatingStatement + 1);
  }
}
