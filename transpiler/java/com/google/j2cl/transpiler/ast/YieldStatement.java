/*
 * Copyright 2024 Google Inc.
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
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Class for a yield statement. */
@Visitable
public class YieldStatement extends Statement {
  @Visitable Expression expression;

  private YieldStatement(SourcePosition sourcePosition, Expression expression) {
    super(sourcePosition);
    this.expression = checkNotNull(expression);
  }

  public Expression getExpression() {
    return expression;
  }

  @Override
  public YieldStatement clone() {
    return YieldStatement.newBuilder()
        .setExpression(expression.clone())
        .setSourcePosition(getSourcePosition())
        .build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_YieldStatement.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for ReturnStatement. */
  public static class Builder {
    private Expression expression;
    private SourcePosition sourcePosition;

    public static Builder from(YieldStatement yieldStatement) {
      return newBuilder()
          .setExpression(yieldStatement.getExpression())
          .setSourcePosition(yieldStatement.getSourcePosition());
    }

    @CanIgnoreReturnValue
    public Builder setExpression(Expression expression) {
      this.expression = expression;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public YieldStatement build() {
      return new YieldStatement(sourcePosition, expression);
    }
  }
}
