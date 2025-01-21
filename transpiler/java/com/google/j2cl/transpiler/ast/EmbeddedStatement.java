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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/**
 * Represents a statement embedded as an expression.
 *
 * <p>Useful to represent constructs like parameterless IIFEs from JavaScript and to embed
 * statements in expressions for the wasm backend.
 */
@Visitable
public class EmbeddedStatement extends Expression {
  @Visitable Statement statement;

  @Visitable TypeDescriptor typeDescriptor;

  private EmbeddedStatement(Statement statement, TypeDescriptor typeDescriptor) {
    this.statement = checkNotNull(statement);
    this.typeDescriptor = checkNotNull(typeDescriptor);
  }

  public Statement getStatement() {
    return statement;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public Precedence getPrecedence() {
    return Precedence.HIGHEST;
  }

  @Override
  public EmbeddedStatement clone() {
    return new EmbeddedStatement(statement.clone(), typeDescriptor);
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_EmbeddedStatement.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for LabeledStatement. */
  public static class Builder {
    private Statement statement;
    private TypeDescriptor typeDescriptor;

    public static Builder from(EmbeddedStatement labeledStatement) {
      return newBuilder()
          .setStatement(labeledStatement.getStatement())
          .setTypeDescriptor(labeledStatement.getTypeDescriptor());
    }

    @CanIgnoreReturnValue
    public Builder setStatement(Statement statement) {
      this.statement = statement;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setTypeDescriptor(TypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    public EmbeddedStatement build() {
      return new EmbeddedStatement(statement, typeDescriptor);
    }
  }
}
