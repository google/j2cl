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

import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.transpiler.ast.Expression.Precedence;

/**
 * Class for postfix operator.
 */
public enum PostfixOperator implements Operator {
  INCREMENT(Precedence.POSTFIX, "++", BinaryOperator.PLUS),
  DECREMENT(Precedence.POSTFIX, "--", BinaryOperator.MINUS),
  NOT_NULL_ASSERTION(Precedence.NOT_NULL_ASSERTION, "!!");

  private final Precedence precedence;
  private final String symbol;
  private final BinaryOperator underlyingBinaryOperator;

  PostfixOperator(Precedence precedence, String symbol, BinaryOperator underlyingBinaryOperator) {
    this.precedence = precedence;
    this.symbol = symbol;
    this.underlyingBinaryOperator = underlyingBinaryOperator;
  }

  PostfixOperator(Precedence precedence, String symbol) {
    this(precedence, symbol, null);
  }

  public Precedence getPrecedence() {
    return precedence;
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

  @Override
  public BinaryOperator getUnderlyingBinaryOperator() {
    return underlyingBinaryOperator;
  }

  /** Returns the corresponding prefix operator. */
  public PrefixOperator toPrefixOperator() {
    switch (this) {
      case DECREMENT:
        return PrefixOperator.DECREMENT;
      case INCREMENT:
        return PrefixOperator.INCREMENT;
      default:
        throw new InternalCompilerError("Unexpected prefix operator: %s.", this);
    }
  }
}
