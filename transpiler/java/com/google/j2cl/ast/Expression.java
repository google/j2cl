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

import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.SourcePosition;

/** Base class for expressions. */
@Visitable
public abstract class Expression extends Node implements Cloneable<Expression> {

  /** Returns the type descriptor of the value that is returned by this expression. */
  public abstract TypeDescriptor getTypeDescriptor();

  /**
   * Returns true if the expression can be evaluated multiple times and always results in the same
   * value.
   *
   * <p>Note: that the expression might have side effects (e.g. cause some class initializers to
   * run). An expression is idempotent if when evaluated in the same state multiple times yields the
   * same resulting state and value.
   */
  public boolean isIdempotent() {
    return false;
  }

  /**
   * Returns true if the expression can be used in the left hand side of an assignment. {@see JLS
   * 15.26}
   */
  public boolean isLValue() {
    return false;
  }

  /** Creates an ExpressionStatement with this expression as its code */
  public ExpressionStatement makeStatement(SourcePosition sourcePosition) {
    return new ExpressionStatement(sourcePosition, this);
  }

  /** Returns the expression enclosed as an expression with a comment. */
  public ExpressionWithComment withComment(String comment) {
    return new ExpressionWithComment(this, comment);
  }

  /** Prefix expression with a spread unary operator. */
  public UnaryExpression prefixSpread() {
    return prefix(PrefixOperator.SPREAD);
  }

  /** Prefix expression with a plus unary operator. */
  public UnaryExpression prefixPlus() {
    return prefix(PrefixOperator.PLUS);
  }

  /** Prefix expression with a not unary operator. */
  public UnaryExpression prefixNot() {
    return prefix(PrefixOperator.NOT);
  }

  /** Returns expression prefixed with unary operator {@code prefixOperator}. */
  public UnaryExpression prefix(PrefixOperator prefixOperator) {
    // TODO(b/67753876): Remove explicit parenthesis once J2cl handles precedence.
    // Parenthesize the operand to enforce the correct precedence unless it is a prefix expression.
    Expression operand = this instanceof PrefixExpression ? this : this.parenthesize();
    return PrefixExpression.newBuilder().setOperator(prefixOperator).setOperand(operand).build();
  }

  /** Return the expression enclosed in parenthesis. */
  public Expression parenthesize() {
    // TODO(b/67753876): Remove explicit parenthesis insertion once J2cl handles precedence.
    if (this.areEnclosingParenthesisUnnecessary()) {
      return this;
    }
    return MultiExpression.newBuilder().setExpressions(this).build();
  }

  /**
   * Returns true if it is guaranteed that removing parenthesis around this expression won't change
   * the meaning of expressions enclosing it.
   */
  public boolean areEnclosingParenthesisUnnecessary() {
    return false;
  }

  @Override
  public abstract Expression clone();

  @Override
  public Node accept(Processor processor) {
    return Visitor_Expression.visit(processor, this);
  }

}
