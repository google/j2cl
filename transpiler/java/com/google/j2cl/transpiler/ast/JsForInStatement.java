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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import javax.annotation.Nullable;

/** JavaScript for..in loop. */
@Visitable
public class JsForInStatement extends LoopStatement {
  @Visitable Variable loopVariable;
  @Visitable Expression iterableExpression;
  @Visitable Statement body;

  private JsForInStatement(
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
  public JsForInStatement clone() {
    Variable loopVariable = getLoopVariable();
    Variable clonedLoopVariable = loopVariable.clone();
    Statement clonedBody =
        AstUtils.replaceDeclarations(
            ImmutableList.of(loopVariable),
            ImmutableList.of(clonedLoopVariable),
            getBody().clone());
    return JsForInStatement.newBuilder()
        .setLoopVariable(clonedLoopVariable)
        .setIterableExpression(iterableExpression.clone())
        .setBody(clonedBody)
        .setSourcePosition(getSourcePosition())
        .build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_JsForInStatement.visit(processor, this);
  }

  @Override
  Builder toBuilder() {
    return new Builder(this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for JsForInStatement. */
  public static class Builder extends LoopStatement.Builder<Builder, JsForInStatement> {
    private Variable loopVariable;
    private Expression iterableExpression;

    public static Builder from(JsForInStatement forInStatement) {
      return new Builder(forInStatement)
          .setLoopVariable(forInStatement.getLoopVariable())
          .setIterableExpression(forInStatement.getIterableExpression());
    }

    public static Builder from(ForEachStatement forEachStatement) {
      return new Builder(forEachStatement)
          .setLoopVariable(forEachStatement.getLoopVariable())
          .setIterableExpression(forEachStatement.getIterableExpression());
    }

    private Builder() {}

    private Builder(LoopStatement loopStatement) {
      super(loopStatement);
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
    protected JsForInStatement doCreateInvocation(
        Expression conditionExpression, Statement body, SourcePosition sourcePosition) {
      checkState(conditionExpression == null);
      return new JsForInStatement(sourcePosition, loopVariable, iterableExpression, body);
    }
  }
}
