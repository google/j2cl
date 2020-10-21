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
package com.google.j2cl.transpiler.backend.wasm;

import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.util.Collection;

/** Transforms Statements into WASM code. */
class StatementTranspiler {
  SourceBuilder builder;

  public StatementTranspiler(SourceBuilder builder) {
    this.builder = builder;
  }

  public void renderStatements(Collection<Statement> statements) {
    statements.forEach(
        s -> {
          builder.newLine();
          renderStatement(s);
        });
  }

  public void renderStatement(Statement statement) {
    class SourceTransformer extends AbstractVisitor {

      @Override
      public boolean enterStatement(Statement assertStatement) {
        builder.append(";;; unimplemented statement " + assertStatement.getClass().getSimpleName());
        return true;
      }

      @Override
      public boolean enterBlock(Block block) {
        builder.openParens();
        builder.append("block");
        renderStatements(block.getStatements());
        builder.closeParens();
        return false;
      }

      @Override
      public boolean enterExpressionStatement(ExpressionStatement expressionStatement) {
        builder.emitWithMapping(
            expressionStatement.getSourcePosition(),
            () -> {
              renderExpression(expressionStatement.getExpression());
              builder.append(";");
            });
        return false;
      }

      @Override
      public String toString() {
        return builder.build();
      }

      private void renderExpression(Expression expression) {
        ExpressionTranspiler.render(expression, builder);
      }
    }

    statement.accept(new SourceTransformer());
  }
}
