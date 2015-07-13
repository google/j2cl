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
package com.google.j2cl.ast;

import com.google.common.base.Preconditions;
import com.google.j2cl.ast.processors.Visitable;

import javax.annotation.Nullable;

/**
 * Assert Statement.
 */
@Visitable
public class AssertStatement extends Statement {
  @Visitable Expression expression;
  @Visitable @Nullable Expression message;

  public AssertStatement(Expression expression, Expression message) {
    Preconditions.checkNotNull(expression);
    this.expression = expression;
    this.message = message;
  }

  public Expression getExpression() {
    return expression;
  }

  public Expression getMessage() {
    return message;
  }

  public void setExpression(Expression expression) {
    this.expression = expression;
  }

  public void setMessage(Expression message) {
    this.message = message;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_AssertStatement.visit(processor, this);
  }
}
