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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.ast.annotations.Visitable;

/**
 * Class for postfix unary expressions.
 */
@Visitable
public class PostfixExpression extends UnaryExpression {
  private PostfixOperator operator;

  private PostfixExpression(
      TypeDescriptor typeDescriptor, Expression operand, PostfixOperator operator) {
    super(checkNotNull(typeDescriptor), operand);
    this.operator = checkNotNull(operator);
  }

  @Override
  public PostfixOperator getOperator() {
    return operator;
  }

  @Override
  public PostfixExpression clone() {
    return new PostfixExpression(getTypeDescriptor(), getOperand().clone(), operator);
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_PostfixExpression.visit(processor, this);
  }

  @Override
  Builder createBuilder() {
    return newBuilder();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * A Builder for postfix unary expressions.
   */
  public static class Builder extends UnaryExpression.Builder {

    @Override
    PostfixExpression doBuild(
        TypeDescriptor typeDescriptor, Expression operand, Operator operator) {
      return new PostfixExpression(typeDescriptor, operand, (PostfixOperator) operator);
    }
  }
}
