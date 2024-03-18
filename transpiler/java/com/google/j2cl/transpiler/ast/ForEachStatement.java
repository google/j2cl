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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import javax.annotation.Nullable;

/** For each Statement. */
@Visitable
public class ForEachStatement extends LoopStatement {
  @Visitable Variable loopVariable;
  @Visitable Expression iterableExpression;
  @Visitable Statement body;

  private ForEachStatement(
      SourcePosition sourcePosition,
      Variable loopVariable,
      Expression iterableExpression,
      Statement body) {
    super(sourcePosition);
    this.loopVariable = checkNotNull(loopVariable);
    this.iterableExpression = checkNotNull(iterableExpression);
    this.body = checkNotNull(body);
  }

  public Variable getLoopVariable() {
    return loopVariable;
  }

  public Expression getIterableExpression() {
    return iterableExpression;
  }

  @Override
  @Nullable
  public Expression getConditionExpression() {
    return null;
  }

  @Override
  public Statement getBody() {
    return body;
  }

  @Override
  public ForEachStatement clone() {
    Variable loopVariable = getLoopVariable();
    Variable clonedLoopVariable = loopVariable.clone();
    Statement clonedBody =
        AstUtils.replaceDeclarations(
            ImmutableList.of(loopVariable),
            ImmutableList.of(clonedLoopVariable),
            getBody().clone());
    return ForEachStatement.newBuilder()
        .setLoopVariable(clonedLoopVariable)
        .setIterableExpression(iterableExpression.clone())
        .setBody(clonedBody)
        .setSourcePosition(getSourcePosition())
        .build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_ForEachStatement.visit(processor, this);
  }

  @Override
  Builder toBuilder() {
    return new Builder(this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for ForEachStatement. */
  public static class Builder extends LoopStatement.Builder<Builder, ForEachStatement> {
    private Variable loopVariable;
    private Expression iterableExpression;

    public static Builder from(ForEachStatement forEachStatement) {
      return new Builder(forEachStatement);
    }

    private Builder() {}

    private Builder(ForEachStatement forEachStatement) {
      super(forEachStatement);
      setLoopVariable(forEachStatement.getLoopVariable());
      setIterableExpression(forEachStatement.getIterableExpression());
    }

    @CanIgnoreReturnValue
    public Builder setLoopVariable(Variable loopVariable) {
      this.loopVariable = loopVariable;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setIterableExpression(Expression iterableExpression) {
      this.iterableExpression = iterableExpression;
      return this;
    }

    @Override
    protected ForEachStatement doCreateInvocation(
        Expression conditionExpression, Statement body, SourcePosition sourcePosition) {
      checkState(conditionExpression == null);
      return new ForEachStatement(sourcePosition, loopVariable, iterableExpression, body);
    }
  }

}
