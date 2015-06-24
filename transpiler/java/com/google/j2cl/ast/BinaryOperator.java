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
 * Binary Operator.
 */
public enum BinaryOperator {
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
  XOR("^"),
  AND("&"),
  OR("|"),
  CONDITIONAL_AND("&&"),
  CONDITIONAL_OR("||"),
  ASSIGN("="),
  PLUS_ASSIGN("+="),
  MINUS_ASSIGN("-="),
  TIMES_ASSIGN("*="),
  DIVIDE_ASSIGN("/="),
  BIT_AND_ASSIGN("&="),
  BIT_OR_ASSIGN("|="),
  BIT_XOR_ASSIGN("^="),
  REMAINDER_ASSIGN("%="),
  LEFT_SHIFT_ASSIGN("<<="),
  RIGHT_SHIFT_SIGNED_ASSIGN(">>="),
  RIGHT_SHIFT_UNSIGNED_ASSIGN(">>>=");
  private String symbol;

  BinaryOperator(String symbol) {
    this.symbol = symbol;
  }

  public String getSymbol() {
    return symbol;
  }

  @Override
  public String toString() {
    return symbol;
  }
}
