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

/** DoWhile Statement. */
@Visitable
public class DoWhileStatement extends LoopStatement {
  // The order of the @Visitable fields here should follow the ordering in the source code to
  // ensure that the traversal is done according with the source ordering.
  @Visitable Statement body;
  @Visitable Expression conditionExpression;

  public DoWhileStatement(
      SourcePosition sourcePosition, Expression conditionExpression, Statement body) {
    super(sourcePosition);
    this.conditionExpression = checkNotNull(conditionExpression);
    this.body = checkNotNull(body);
  }

  @Override
  public Expression getConditionExpression() {
    return conditionExpression;
  }

  @Override
  public Statement getBody() {
    return body;
  }

  @Override
  public DoWhileStatement clone() {
    return new DoWhileStatement(
        getSourcePosition(), conditionExpression.clone(), AstUtils.clone(body));
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_DoWhileStatement.visit(processor, this);
  }

  @Override
  Builder toBuilder() {
    return new Builder(this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for DoWhileStatement. */
  public static class Builder extends LoopStatement.Builder<Builder, DoWhileStatement> {

    public static Builder from(DoWhileStatement doWhileStatement) {
      return new Builder(doWhileStatement);
    }

    private Builder() {}

    private Builder(DoWhileStatement doWhileStatement) {
      super(doWhileStatement);
    }

    @Override
    protected DoWhileStatement doCreateInvocation(
        Expression conditionExpression, Statement body, SourcePosition sourcePosition) {
      return new DoWhileStatement(sourcePosition, conditionExpression, body);
    }
  }
}
