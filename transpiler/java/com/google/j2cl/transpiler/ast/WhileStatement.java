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

/** While Statement. */
@Visitable
public class WhileStatement extends Statement {
  @Visitable Expression conditionExpression;
  @Visitable Statement body;

  private WhileStatement(
      SourcePosition sourcePosition, Expression conditionExpression, Statement body) {
    super(sourcePosition);
    this.conditionExpression = checkNotNull(conditionExpression);
    this.body = checkNotNull(body);
  }

  public Expression getConditionExpression() {
    return conditionExpression;
  }

  public Statement getBody() {
    return body;
  }

  @Override
  public WhileStatement clone() {
    return new WhileStatement(
        getSourcePosition(), conditionExpression.clone(), AstUtils.clone(body));
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_WhileStatement.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for WhileStatement. */
  public static class Builder {
    private Expression conditionExpression;
    private Statement body;
    private SourcePosition sourcePosition;

    public static Builder from(WhileStatement whileStatement) {
      return new Builder()
          .setSourcePosition(whileStatement.getSourcePosition())
          .setConditionExpression(whileStatement.getConditionExpression())
          .setBody(whileStatement.getBody());
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public Builder setConditionExpression(Expression conditionExpression) {
      this.conditionExpression = conditionExpression;
      return this;
    }

    public Builder setBody(Statement body) {
      this.body = body;
      return this;
    }

    public WhileStatement build() {
      return new WhileStatement(sourcePosition, conditionExpression, body);
    }
  }
}
