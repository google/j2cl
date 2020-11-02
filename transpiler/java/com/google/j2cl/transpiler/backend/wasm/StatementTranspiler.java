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
import com.google.j2cl.transpiler.ast.ThrowStatement;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.util.Collection;

/** Transforms Statements into WASM code. */
class StatementTranspiler {
  SourceBuilder builder;
  GenerationEnvironment environment;

  public StatementTranspiler(SourceBuilder builder, GenerationEnvironment environment) {
    this.builder = builder;
    this.environment = environment;
  }

  public void renderStatements(Collection<Statement> statements) {
    statements.forEach(
        s -> {
          builder.newLine();
          builder.appendln(renderAsSingleLineComments(s.toString()));
          renderStatement(s);
        });
  }

  /**
   * Transforms a comment that can have multiple lines into multiple single line comments.
   *
   * <p>WASM does not have multiline comments, so comments that span multiple lines need to be
   * rendered as multiple single line comments.
   */
  private static String renderAsSingleLineComments(String s) {
    return ";; " + s.replace("\n", "\n;; ");
  }

  public void renderStatement(Statement statement) {
    class SourceTransformer extends AbstractVisitor {

      @Override
      public boolean enterStatement(Statement assertStatement) {
        builder.appendln(
            ";; unimplemented statement " + assertStatement.getClass().getSimpleName());
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
        Expression expression = expressionStatement.getExpression();
        builder.emitWithMapping(
            expressionStatement.getSourcePosition(),
            () -> {
              renderExpression(expression);
            });
        if (!TypeDescriptors.isPrimitiveVoid(expression.getTypeDescriptor())) {
          builder.newLine();
          // Remove the result of the expression from the stack.
          builder.append("drop");
        }
        return false;
      }

      @Override
      public boolean enterThrowStatement(ThrowStatement throwStatement) {
        renderExpression(throwStatement.getExpression());
        builder.newLine();
        // TODO(rluble): This is a nominal placeholder implementation that throws to JavaScript
        // until WASM exception handling is added.
        builder.emitWithMapping(
            throwStatement.getSourcePosition(), () -> builder.append("unreachable"));
        return false;
      }

      private void renderExpression(Expression expression) {
        ExpressionTranspiler.render(expression, builder, environment);
      }
    }

    statement.accept(new SourceTransformer());
  }
}
