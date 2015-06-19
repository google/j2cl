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

/**
 * Assert Statement.
 */
public class AssertStatement extends Statement {
  @Visitable Expression expression;
  @Visitable Expression message;

  public AssertStatement(Expression expression, Expression message) {
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
  public AssertStatement accept(Visitor visitor) {
    return VisitorAssertStatement.visit(visitor, this);
  }
}
