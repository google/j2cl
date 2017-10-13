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

/**
 * Binary Operator.
 */
public enum BinaryOperator implements Operator {
  TIMES("*"),
  DIVIDE("/"),
  REMAINDER("%"),
  PLUS("+"),
  MINUS("-"),
  LEFT_SHIFT("<<"),
  RIGHT_SHIFT_SIGNED(">>"),
  RIGHT_SHIFT_UNSIGNED(">>>"),
  LESS("<"),
  GREATER(">"),
  LESS_EQUALS("<="),
  GREATER_EQUALS(">="),
  EQUALS("=="),
  NOT_EQUALS("!="),
  BIT_XOR("^"),
  BIT_AND("&"),
  BIT_OR("|"),
  CONDITIONAL_AND("&&"),
  CONDITIONAL_OR("||"),
  // Assignment operators.
  ASSIGN("="),
  PLUS_ASSIGN("+=", PLUS),
  MINUS_ASSIGN("-=", MINUS),
  TIMES_ASSIGN("*=", TIMES),
  DIVIDE_ASSIGN("/=", DIVIDE),
  BIT_AND_ASSIGN("&=", BIT_AND),
  BIT_OR_ASSIGN("|=", BIT_OR),
  BIT_XOR_ASSIGN("^=", BIT_XOR),
  REMAINDER_ASSIGN("%=", REMAINDER),
  LEFT_SHIFT_ASSIGN("<<=", LEFT_SHIFT),
  RIGHT_SHIFT_SIGNED_ASSIGN(">>=", RIGHT_SHIFT_SIGNED),
  RIGHT_SHIFT_UNSIGNED_ASSIGN(">>>=", RIGHT_SHIFT_UNSIGNED);

  private final String symbol;
  private final BinaryOperator underlyingBinaryOperator;

  BinaryOperator(String symbol) {
    this(symbol, null);
  }

  BinaryOperator(String symbol, BinaryOperator underlyingBinaryOperator) {
    this.symbol = checkNotNull(symbol);
    this.underlyingBinaryOperator = underlyingBinaryOperator;
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
    return isAssignmentOperator();
  }

  public boolean isAssignmentOperator() {
    return this == ASSIGN || underlyingBinaryOperator != null;
  }

  public boolean isCompoundAssignment() {
    return this != ASSIGN && hasSideEffect();
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

  public boolean isLeftAssociative() {
    // All binary operators except for assignment operators are left-associative;
    return !isAssignmentOperator();
  }

  @Override
  public String toString() {
    return symbol;
  }
}
