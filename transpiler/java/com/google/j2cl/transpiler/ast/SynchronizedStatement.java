/*
 * Copyright 2015 Google Inc.
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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/**
 * Synchronized statement.
 */
@Visitable
public class SynchronizedStatement extends Statement {
  @Visitable Expression expression;
  @Visitable Block body;

  private SynchronizedStatement(SourcePosition sourcePosition, Expression expression, Block body) {
    super(sourcePosition);
    this.expression = checkNotNull(expression);
    this.body = checkNotNull(body);
  }

  public Expression getExpression() {
    return expression;
  }

  public Block getBody() {
    return body;
  }

  @Override
  public SynchronizedStatement clone() {
    return new SynchronizedStatement(getSourcePosition(), expression.clone(), body.clone());
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_SynchronizedStatement.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for synchronized statement. */
  public static class Builder {
    private SourcePosition sourcePosition;
    private Expression expression;
    private Block body;

    public static Builder from(SynchronizedStatement synchronizedStatement) {
      return new Builder()
          .setSourcePosition(synchronizedStatement.getSourcePosition())
          .setExpression(synchronizedStatement.getExpression())
          .setBody(synchronizedStatement.getBody());
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public Builder setExpression(Expression expression) {
      this.expression = expression;
      return this;
    }

    public Builder setBody(Block body) {
      this.body = body;
      return this;
    }

    public SynchronizedStatement build() {
      return new SynchronizedStatement(sourcePosition, expression, body);
    }
  }
}
