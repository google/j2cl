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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/**
 * Represents multiple ordered statements used as an expression.
 *
 * <p>This kind of construct is ubiquitous in Kotlin.
 */
@Visitable
public class StatementAsExpression extends Expression {
  @Visitable Statement statement;

  private StatementAsExpression(Statement statement) {
    this.statement = checkNotNull(statement);
  }

  public Statement getStatement() {
    return statement;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return getStatementTypeDescriptor(statement);
  }

  private TypeDescriptor getStatementTypeDescriptor(Statement statement) {
    // TODO(rluble): move the type descriptor to the statement, maybe.
    if (statement instanceof Block) {
      Block block = (Block) statement;
      if (block.getStatements().isEmpty()) {
        return PrimitiveTypes.VOID;
      }
      return getStatementTypeDescriptor(Iterables.getLast(block.getStatements()));
    }
    if (statement instanceof ExpressionStatement) {
      ExpressionStatement expressionStatement = (ExpressionStatement) statement;
      return expressionStatement.getExpression().getTypeDescriptor();
    }

    if (statement instanceof LocalClassDeclarationStatement) {
      return PrimitiveTypes.VOID;
    }
    throw new IllegalStateException("StatementAsExpression unhandled statement " + statement);
  }

  @Override
  public TypeDescriptor getDeclaredTypeDescriptor() {
    // TODO(rluble): return the declared type descriptor.
    return getTypeDescriptor();
  }

  @Override
  public Precedence getPrecedence() {
    // StatementAsExpressions are emitted always with brackets, so no need for extra parens.
    return Precedence.HIGHEST;
  }

  @Override
  public Expression clone() {
    return StatementAsExpression.newBuilder().setStatement(statement.clone()).build();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_StatementAsExpression.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for MultiExpression. */
  public static class Builder {
    private Statement statement = null;

    public Builder setStatement(Statement statement) {
      this.statement = statement;
      return this;
    }

    public Expression build() {
      return new StatementAsExpression(statement);
    }
  }
}
