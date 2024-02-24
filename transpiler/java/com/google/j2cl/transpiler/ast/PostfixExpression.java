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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/**
 * Class for postfix unary expressions.
 */
@Visitable
public class PostfixExpression extends UnaryExpression {
  private final PostfixOperator operator;

  private PostfixExpression(Expression operand, PostfixOperator operator) {
    super(operand);
    this.operator = checkNotNull(operator);
    checkArgument(!operator.hasSideEffect() || operand.isLValue());
  }

  @Override
  public PostfixOperator getOperator() {
    return operator;
  }

  @Override
  public Precedence getPrecedence() {
    return operator.getPrecedence();
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return operator == PostfixOperator.NOT_NULL_ASSERTION
        ? operand.getTypeDescriptor().toNonNullable()
        : super.getTypeDescriptor();
  }

  // TODO(b/236987392): Remove this method {@code TypeDescriptor.canBeNull()} is fixed.
  @Override
  public boolean canBeNull() {
    return super.canBeNull() && operator != PostfixOperator.NOT_NULL_ASSERTION;
  }

  @Override
  public PostfixExpression clone() {
    return new PostfixExpression(getOperand().clone(), operator);
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_PostfixExpression.visit(processor, this);
  }

  @Override
  Builder createBuilder() {
    return newBuilder();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for postfix unary expressions. */
  public static class Builder extends UnaryExpression.Builder<Builder, PostfixExpression> {
    public static Builder from(UnaryExpression expression) {
      return newBuilder().setOperand(expression.getOperand()).setOperator(expression.getOperator());
    }

    @Override
    PostfixExpression doBuild(Expression operand, Operator operator) {
      return new PostfixExpression(operand, (PostfixOperator) operator);
    }
  }
}
