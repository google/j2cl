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
 * Binary operator expression.
 */
@Visitable
public class BinaryExpression extends Expression {
  private TypeDescriptor typeDescriptor;
  @Visitable Expression leftOperand;
  private BinaryOperator operator;
  @Visitable Expression rightOperand;

  public BinaryExpression(
      TypeDescriptor typeDescriptor,
      Expression leftOperand,
      BinaryOperator operator,
      Expression rightOperand) {
    this.typeDescriptor = checkNotNull(typeDescriptor);
    this.leftOperand = checkNotNull(leftOperand);
    this.operator = checkNotNull(operator);
    this.rightOperand = checkNotNull(rightOperand);
  }

  public Expression getLeftOperand() {
    return leftOperand;
  }

  public BinaryOperator getOperator() {
    return operator;
  }

  public Expression getRightOperand() {
    return rightOperand;
  }

  public void setLeftOperand(Expression leftOperand) {
    this.leftOperand = leftOperand;
  }

  public void setOperator(BinaryOperator operator) {
    this.operator = operator;
  }

  public void setRightOperand(Expression rightOperand) {
    this.rightOperand = rightOperand;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_BinaryExpression.visit(processor, this);
  }

  /**
   * A Builder for binary expressions.
   */
  public static class Builder {
    private BinaryOperator operator;
    private TypeDescriptor typeDescriptor;
    private Expression leftOperand;
    private Expression rightOperand;

    public static Builder from(BinaryExpression expression) {
      Builder builder =
          new Builder()
              .setLeftOperand(expression.getLeftOperand())
              .setOperator(expression.getOperator())
              .setRightOperand(expression.getRightOperand())
              .setTypeDescriptor(expression.getTypeDescriptor());
      return builder;
    }

    public static Builder asAssignmentTo(Variable variable) {
      Builder builder =
          new Builder()
              .setLeftOperand(variable.getReference())
              .setTypeDescriptor(variable.getTypeDescriptor())
              .setOperator(BinaryOperator.ASSIGN);
      return builder;
    }

    public static Builder asAssignmentTo(Expression lvalue) {
      Builder builder =
          new Builder()
              .setLeftOperand(lvalue)
              .setTypeDescriptor(lvalue.getTypeDescriptor())
              .setOperator(BinaryOperator.ASSIGN);
      return builder;
    }

    public Builder setLeftOperand(Expression operand) {
      this.leftOperand = operand;
      return this;
    }

    public Builder setRightOperand(Expression operand) {
      this.rightOperand = operand;
      return this;
    }

    public Builder setOperator(BinaryOperator operator) {
      this.operator = operator;
      return this;
    }

    public Builder setTypeDescriptor(TypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    public final BinaryExpression build() {
      return new BinaryExpression(typeDescriptor, leftOperand, operator, rightOperand);
    }
  }
}
