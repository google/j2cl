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
 * Binary operator expression.
 */
public class BinaryExpression extends Expression {
  private Expression leftOperand;
  private BinaryOperator operator;
  private Expression rightOperand;

  public BinaryExpression(
      Expression leftOperand, BinaryOperator operator, Expression rightOperand) {
    this.leftOperand = leftOperand;
    this.operator = operator;
    this.rightOperand = rightOperand;
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
  public String toString() {
    return String.format(
        "%s %s %s", leftOperand.toString(), operator.toString(), rightOperand.toString());
  }
}
