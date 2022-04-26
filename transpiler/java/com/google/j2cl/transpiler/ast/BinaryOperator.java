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

import com.google.j2cl.transpiler.ast.Expression.Precedence;

/**
 * Binary Operator.
 */
public enum BinaryOperator implements Operator {
  // Precedence for JavaScript, see.
  TIMES(Precedence.MULTIPLICATIVE, "*"),
  DIVIDE(Precedence.MULTIPLICATIVE, "/"),
  REMAINDER(Precedence.MULTIPLICATIVE, "%"),
  PLUS(Precedence.ADDITIVE, "+"),
  MINUS(Precedence.ADDITIVE, "-"),
  LEFT_SHIFT(Precedence.SHIFT_OPERATOR, "<<"),
  RIGHT_SHIFT_SIGNED(Precedence.SHIFT_OPERATOR, ">>"),
  RIGHT_SHIFT_UNSIGNED(Precedence.SHIFT_OPERATOR, ">>>"),
  LESS(Precedence.RELATIONAL, "<"),
  GREATER(Precedence.RELATIONAL, ">"),
  LESS_EQUALS(Precedence.RELATIONAL, "<="),
  GREATER_EQUALS(Precedence.RELATIONAL, ">="),
  EQUALS(Precedence.EQUALITY, "=="),
  NOT_EQUALS(Precedence.EQUALITY, "!="),
  BIT_XOR(Precedence.BITWISE_XOR, "^"),
  BIT_AND(Precedence.BITWISE_AND, "&"),
  BIT_OR(Precedence.BITWISE_OR, "|"),
  CONDITIONAL_AND(Precedence.LOGICAL_AND, "&&"),
  CONDITIONAL_OR(Precedence.LOGICAL_OR, "||"),
  // Assignment operators.
  ASSIGN(Precedence.ASSIGNMENT, "="),
  PLUS_ASSIGN(Precedence.ASSIGNMENT, "+=", PLUS),
  MINUS_ASSIGN(Precedence.ASSIGNMENT, "-=", MINUS),
  TIMES_ASSIGN(Precedence.ASSIGNMENT, "*=", TIMES),
  DIVIDE_ASSIGN(Precedence.ASSIGNMENT, "/=", DIVIDE),
  BIT_AND_ASSIGN(Precedence.ASSIGNMENT, "&=", BIT_AND),
  BIT_OR_ASSIGN(Precedence.ASSIGNMENT, "|=", BIT_OR),
  BIT_XOR_ASSIGN(Precedence.ASSIGNMENT, "^=", BIT_XOR),
  REMAINDER_ASSIGN(Precedence.ASSIGNMENT, "%=", REMAINDER),
  LEFT_SHIFT_ASSIGN(Precedence.ASSIGNMENT, "<<=", LEFT_SHIFT),
  RIGHT_SHIFT_SIGNED_ASSIGN(Precedence.ASSIGNMENT, ">>=", RIGHT_SHIFT_SIGNED),
  RIGHT_SHIFT_UNSIGNED_ASSIGN(Precedence.ASSIGNMENT, ">>>=", RIGHT_SHIFT_UNSIGNED);

  private final String symbol;
  private final BinaryOperator underlyingBinaryOperator;
  private final Expression.Precedence precedence;

  BinaryOperator(Expression.Precedence precedence, String symbol) {
    this(precedence, symbol, null);
  }

  BinaryOperator(
      Expression.Precedence precedence, String symbol, BinaryOperator underlyingBinaryOperator) {
    this.symbol = checkNotNull(symbol);
    this.underlyingBinaryOperator = underlyingBinaryOperator;
    this.precedence = precedence;
  }

  @Override
  public BinaryOperator getUnderlyingBinaryOperator() {
    return underlyingBinaryOperator;
  }

  @Override
  public String getSymbol() {
    return symbol;
  }

  @Override
  public boolean hasSideEffect() {
    return isSimpleOrCompoundAssignment();
  }

  /** Returns true for plain assignment but not true compound assignment operators. */
  public boolean isSimpleAssignment() {
    return this == ASSIGN;
  }

  /** Returns true for plain assignment and compound assignment operators. */
  public boolean isSimpleOrCompoundAssignment() {
    return isSimpleAssignment() || isCompoundAssignment();
  }

  public boolean isCompoundAssignment() {
    return underlyingBinaryOperator != null;
  }

  public boolean isBitwiseOperator() {
    if (isCompoundAssignment()) {
      return getUnderlyingBinaryOperator().isShiftOperator();
    }

    switch (this) {
      case BIT_XOR:
      case BIT_OR:
      case BIT_AND:
        return true;
      default:
        return false;
    }
  }

  public boolean isShiftOperator() {
    if (isCompoundAssignment()) {
      return getUnderlyingBinaryOperator().isShiftOperator();
    }

    switch (this) {
      case LEFT_SHIFT:
      case RIGHT_SHIFT_SIGNED:
      case RIGHT_SHIFT_UNSIGNED:
        return true;
      default:
        return false;
    }
  }

  public boolean isRelationalOperator() {
    switch (this) {
      case LESS:
      case LESS_EQUALS:
      case EQUALS:
      case NOT_EQUALS:
      case GREATER:
      case GREATER_EQUALS:
        return true;
      default:
        return false;
    }
  }

  public boolean isPlusOperator() {
    if (isCompoundAssignment()) {
      return getUnderlyingBinaryOperator().isPlusOperator();
    }
    return this == PLUS;
  }

  /** Returns the precedence of this operator. See {@link Expression#getPrecedence()}. */
  public Expression.Precedence getPrecedence() {
    return precedence;
  }

  @Override
  public String toString() {
    return symbol;
  }
}
