/*
 * Copyright 2016 Google Inc.
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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** A node that represent a closure type cast "/** @type {Type} * / (expression)". */
@Visitable
public class JsDocCastExpression extends Expression {
  @Visitable Expression expression;
  @Visitable TypeDescriptor castTypeDescriptor;

  private JsDocCastExpression(Expression expression, TypeDescriptor castTypeDescriptor) {
    this.expression = checkNotNull(expression);
    this.castTypeDescriptor = checkNotNull(castTypeDescriptor);
  }

  public Expression getExpression() {
    return expression;
  }

  public TypeDescriptor getCastTypeDescriptor() {
    return castTypeDescriptor;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return castTypeDescriptor;
  }

  @Override
  public Precedence getPrecedence() {
    // These are always emitted with parens.
    return Precedence.HIGHEST;
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_JsDocCastExpression.visit(processor, this);
  }

  @Override
  public JsDocCastExpression clone() {
    return new JsDocCastExpression(expression.clone(), castTypeDescriptor);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for JsDocCastExpression */
  public static class Builder {
    private Expression expression;
    private TypeDescriptor castTypeDescriptor;

    public static Builder from(JsDocCastExpression annotation) {
      Builder builder = new Builder();
      builder.expression = annotation.getExpression();
      builder.castTypeDescriptor = annotation.getTypeDescriptor();
      return builder;
    }

    @CanIgnoreReturnValue
    public Builder setExpression(Expression expression) {
      this.expression = expression;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setCastTypeDescriptor(TypeDescriptor castTypeDescriptor) {
      this.castTypeDescriptor = castTypeDescriptor;
      return this;
    }

    public JsDocCastExpression build() {
      return new JsDocCastExpression(
          // Avoid pointlessly nesting type annotations.
          AstUtils.removeJsDocCastIfPresent(expression), castTypeDescriptor);
    }
  }
}
