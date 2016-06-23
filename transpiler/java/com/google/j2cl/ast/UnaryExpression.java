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
 * Class for unary expressions.
 */
@Visitable
public abstract class UnaryExpression extends Expression {
  private TypeDescriptor typeDescriptor;
  @Visitable Expression operand;

  public UnaryExpression(TypeDescriptor typeDescriptor, Expression operand) {
    this.operand = checkNotNull(operand);
    this.typeDescriptor = checkNotNull(typeDescriptor);
  }

  public Expression getOperand() {
    return operand;
  }

  public abstract Operator getOperator();

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_UnaryExpression.visit(processor, this);
  }

  abstract Builder newBuilder();

  /**
   * A Builder for unary expressions.
   */
  public abstract static class Builder {
    private Expression operand;
    private Operator operator;
    private TypeDescriptor typeDescriptor;

    public static Builder from(UnaryExpression expression) {
      Builder builder = expression.newBuilder();
      builder.operand = expression.getOperand();
      builder.typeDescriptor = expression.getTypeDescriptor();
      builder.operator = expression.getOperator();
      return builder;
    }

    public Builder setOperand(Expression operand) {
      this.operand = operand;
      return this;
    }

    public Builder setOperator(Operator operator) {
      this.operator = (PrefixOperator) operator;
      return this;
    }

    public Builder setTypeDescriptor(TypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    public final Expression build() {
      return doBuild(typeDescriptor, operand, operator);
    }

    abstract Expression doBuild(
        TypeDescriptor typeDescriptor, Expression operand, Operator operator);
  }
}
