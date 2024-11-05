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
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Collapses empty fallthrough cases into cases with multiple expressions.
 *
 * <p>This transformation removes common fallthrough cases making the switch statement easier to
 * convert to a switch expression.
 */
public class RemoveEmptyFallthroughSwitchCases extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {

    // Fold empty cases and remove empty trailing cases.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteSwitchStatement(SwitchStatement switchStatement) {
            List<SwitchCase> switchCases = switchStatement.getCases();

            // Fold non-default empty cases.
            Iterator<SwitchCase> iterator = switchCases.iterator();
            List<Expression> caseExpressionsToMove = new ArrayList<>();
            while (iterator.hasNext()) {
              SwitchCase switchCase = iterator.next();
              if (switchCase.isDefault()) {
                // This is the default, remove all accumulated expressions, since by this case
                // being the default they will nonetheless end up here.
                caseExpressionsToMove.clear();
              } else if (switchCase.getStatements().stream().allMatch(Statement::isNoop)) {
                // Accumulate all expressions in the case with no statements, which makes it
                // fallthrough to the next case, to merge with the next case that has statements to
                // execute.
                caseExpressionsToMove.addAll(switchCase.getCaseExpressions());
                iterator.remove();
              } else if (!caseExpressionsToMove.isEmpty()) {
                // Found a case with statements, so merge all the expressions accumulated so far
                // from cases with no statements.
                switchCase.getCaseExpressions().addAll(0, caseExpressionsToMove);
                caseExpressionsToMove.clear();
              }
            }

            // At this point, the only case that could be empty and fallthrough the next case is
            // the default case, which we handle next.
            SwitchCase defaultCase =
                switchCases.stream().filter(SwitchCase::isDefault).findFirst().orElse(null);
            if (defaultCase == null
                || !defaultCase.getStatements().stream().allMatch(Statement::isNoop)) {
              // There is no default or the default is not empty.
              return switchStatement;
            }

            // There is an empty default that will either be combined with the next case or removed
            // if there is no next case.
            int i = switchCases.indexOf(defaultCase);
            if (i + 1 == switchCases.size()) {
              // The default is the last case and is empty, remove.
              switchCases.remove(i);
              return switchStatement;
            }

            // Move all the statements from the next case to the default case.
            defaultCase.getStatements().addAll(switchCases.get(i + 1).getStatements());
            // and remove the case that was folded into the default.
            switchCases.remove(i + 1);
            return switchStatement;
          }
        });
  }
}
