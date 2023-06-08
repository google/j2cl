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
import javax.annotation.Nullable;

/** If Statement. */
@Visitable
public class IfStatement extends Statement {
  @Visitable Expression conditionExpression;
  @Visitable Statement thenStatement;
  @Visitable @Nullable Statement elseStatement;

  private IfStatement(
      SourcePosition sourcePosition,
      Expression conditionExpression,
      Statement thenStatement,
      @Nullable Statement elseStatement) {
    super(sourcePosition);
    this.conditionExpression = checkNotNull(conditionExpression);
    this.thenStatement = checkNotNull(thenStatement);
    this.elseStatement = elseStatement;
  }

  public Expression getConditionExpression() {
    return conditionExpression;
  }

  public Statement getThenStatement() {
    return thenStatement;
  }

  @Nullable
  public Statement getElseStatement() {
    return elseStatement;
  }

  @Override
  public IfStatement clone() {
    return new IfStatement(
        getSourcePosition(),
        conditionExpression.clone(),
        thenStatement.clone(),
        AstUtils.clone(elseStatement));
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_IfStatement.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for IfStatement. */
  public static class Builder {
    private Expression conditionExpression;
    private Statement thenStatement;
    private Statement elseStatement;
    private SourcePosition sourcePosition;

    public static Builder from(IfStatement ifStatement) {
      return new Builder()
          .setSourcePosition(ifStatement.getSourcePosition())
          .setConditionExpression(ifStatement.getConditionExpression())
          .setThenStatement(ifStatement.getThenStatement())
          .setElseStatement(ifStatement.getElseStatement());
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public Builder setConditionExpression(Expression conditionExpression) {
      this.conditionExpression = conditionExpression;
      return this;
    }

    public Builder setThenStatement(Statement thenStatement) {
      this.thenStatement = thenStatement;
      return this;
    }

    public Builder setElseStatement(Statement elseStatement) {
      this.elseStatement = elseStatement;
      return this;
    }

    public IfStatement build() {
      return new IfStatement(sourcePosition, conditionExpression, thenStatement, elseStatement);
    }
  }
}
