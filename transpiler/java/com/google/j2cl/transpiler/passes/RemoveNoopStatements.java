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

import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchCase;

/**
 * Removes statements that have no effect.
 *
 * <p>Some of the normalization passes might leave statements that are empty or just a literal, this
 * pass performs the cleanup.
 */
public class RemoveNoopStatements extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // In general, list of statements always appear enclosed in a block, such is the case of the
    // statements in the body of loops, the body of a try statement, etc.
    // However there is an exception to this rule; SwitchCase. SwitchCase is the only other node in
    // the AST that contains a list of statements. That list of statements can not be modeled as a
    // Block due to the scoping of variables; since a variable declared in a switch case is in scope
    // in the rest of the cases.
    // All the other constructs that might look as if they have lists of statements (i.e the init
    // and update part of the 'for' loop, the resource declarations in the 'try' statement) are
    // not statements hence cannot contain empty statements.
    // Also empty blocks can only be removed when they are in a list of statements, not when they
    // are the only statement as in "if (...) {}".
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitBlock(Block block) {
            block.getStatements().removeIf(Statement::isNoop);
          }

          @Override
          public void exitSwitchCase(SwitchCase switchCase) {
            switchCase.getStatements().removeIf(Statement::isNoop);
          }
        });
  }
}
