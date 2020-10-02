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

/**
 * Class for prefix operator.
 */
public enum PrefixOperator implements Operator {
  INCREMENT("++", BinaryOperator.PLUS),
  DECREMENT("--", BinaryOperator.MINUS),
  PLUS("+"),
  MINUS("-"),
  COMPLEMENT("~"),
  NOT("!"),
  SPREAD("..."); // Refers to the Javascript ES6 spread operator.

  private final String symbol;
  private final BinaryOperator underlyingBinaryOperator;

  PrefixOperator(String symbol) {
    this(symbol, null);
  }

  PrefixOperator(String symbol, BinaryOperator underlyingBinaryOperator) {
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
    return underlyingBinaryOperator != null;
  }

  @Override
  public String toString() {
    return symbol;
  }

}
