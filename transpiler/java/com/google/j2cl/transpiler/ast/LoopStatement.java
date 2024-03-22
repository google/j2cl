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
package com.google.j2cl.transpiler.ast;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import javax.annotation.Nullable;

/** Common logic for loop construct statement. */
@Visitable
public abstract class LoopStatement extends Statement {

  public LoopStatement(SourcePosition sourcePosition) {
    super(sourcePosition);
  }

  @Nullable
  public abstract Expression getConditionExpression();

  public abstract Statement getBody();

  @Override
  abstract Node acceptInternal(Processor processor);

  abstract LoopStatement.Builder<?, ?> toBuilder();

  /** Common logic for a builder to create loop statements. */
  public abstract static class Builder<T extends Builder<T, L>, L extends LoopStatement> {
    private Expression conditionExpression;
    private Statement body;
    private SourcePosition sourcePosition;

    public static Builder<?, ?> from(LoopStatement loopStatement) {
      return loopStatement.toBuilder();
    }

    @CanIgnoreReturnValue
    public final T setConditionExpression(Expression conditionExpression) {
      this.conditionExpression = conditionExpression;
      return getThis();
    }

    @CanIgnoreReturnValue
    public T setBody(Statement body) {
      this.body = body;
      return getThis();
    }

    @CanIgnoreReturnValue
    public T setBodyStatements(Statement... statements) {
      if (statements.length == 1 && statements[0] instanceof Block) {
        setBody(statements[0]);
      }
      return setBody(Block.newBuilder().setStatements(statements).build());
    }

    @CanIgnoreReturnValue
    public final T setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return getThis();
    }

    @SuppressWarnings("unchecked")
    private T getThis() {
      return (T) this;
    }

    public final L build() {
      return doCreateInvocation(conditionExpression, body, sourcePosition);
    }

    protected abstract L doCreateInvocation(
        Expression conditionExpression, Statement body, SourcePosition sourcePosition);

    protected Builder(LoopStatement loopStatement) {
      setBody(loopStatement.getBody());
      setConditionExpression(loopStatement.getConditionExpression());
      setSourcePosition(loopStatement.getSourcePosition());
    }

    protected Builder() {}
  }
}
