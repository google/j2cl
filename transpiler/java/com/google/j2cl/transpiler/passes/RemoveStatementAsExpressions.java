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
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.StatementAsExpression;
import java.util.ArrayList;
import java.util.List;

/**
 * Removes StatementAsExpressions so that all statements are properly nested within statements only
 */
public class RemoveStatementAsExpressions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteStatementAsExpression(
              StatementAsExpression statementAsExpression) {
            List<Expression> expressions = getExpressions(statementAsExpression.getStatement());

            return MultiExpression.newBuilder().setExpressions(expressions).build();
          }
        });
  }

  private static List<Expression> getExpressions(Statement statement) {
    List<Expression> expressions = new ArrayList<>();
    if (statement instanceof Block) {
      ((Block) statement).getStatements().forEach(s -> expressions.addAll(getExpressions(s)));
    } else if (statement instanceof ExpressionStatement) {
      expressions.add(((ExpressionStatement) statement).getExpression());
    } else {
      throw new UnsupportedOperationException("Statement to Expression conversion not fully");
    }
    return expressions;
  }
}
