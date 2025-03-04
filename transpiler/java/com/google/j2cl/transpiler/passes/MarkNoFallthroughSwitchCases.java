/*
 * Copyright 2025 Google Inc.
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

import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BreakStatement;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ContinueStatement;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.ThrowStatement;
import com.google.j2cl.transpiler.ast.YieldStatement;
import java.util.List;

/** Marks switch cases as no-fallthrough when possible. */
public class MarkNoFallthroughSwitchCases extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public SwitchCase rewriteSwitchCase(SwitchCase switchCase) {
            if (!switchCase.canFallthrough()) {
              return switchCase;
            }
            return SwitchCase.Builder.from(switchCase)
                .setCanFallthrough(canFallthrough(switchCase.getStatements()))
                .build();
          }
        });
  }

  /*
   * A conservative approximation of when a statement is guaranteed to exit the switch clause
   * without falling through the next case clause. In particular labeled statements are completely
   * skipped to avoid reasoning about local jumps in control flow.
   */
  private static boolean canFallthrough(Statement statement) {
    if (statement instanceof ReturnStatement) {
      return false;
    }

    if (statement instanceof YieldStatement) {
      return false;
    }

    if (statement instanceof ThrowStatement) {
      return false;
    }

    if (statement instanceof BreakStatement || statement instanceof ContinueStatement) {
      // Since we are not entering labeled statements, loop statements and other switch statements,
      // these break and continue statement are guaranteed to target outside switch statement.
      return false;
    }

    if (statement instanceof Block) {
      Block block = (Block) statement;
      return canFallthrough(block.getStatements());
    }

    if (statement instanceof IfStatement) {
      IfStatement ifStatement = (IfStatement) statement;
      Statement elseStatement = ifStatement.getElseStatement();
      return elseStatement == null
          || canFallthrough(ifStatement.getThenStatement())
          || canFallthrough(elseStatement);
    }

    return true;
  }

  private static boolean canFallthrough(List<Statement> statements) {
    Statement lastStatement = Iterables.getLast(statements, null);
    return lastStatement == null || canFallthrough(lastStatement);
  }
}
