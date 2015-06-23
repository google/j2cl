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

import com.google.j2cl.ast.processors.Visitable;

/**
 * Class for Postfix Expression.
 */
@Visitable
public class PostfixExpression extends Expression {
  @Visitable Expression operand;
  private PostfixOperator operator;

  public PostfixExpression(Expression operand, PostfixOperator operator) {
    this.operand = operand;
    this.operator = operator;
  }

  public Expression getOperand() {
    return operand;
  }

  public PostfixOperator getOperator() {
    return operator;
  }

  public void setOperand(Expression operand) {
    this.operand = operand;
  }

  public void setOperator(PostfixOperator operator) {
    this.operator = operator;
  }

  @Override
  PostfixExpression accept(Processor processor) {
    return Visitor_PostfixExpression.visit(processor, this);
  }
}
