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

import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Class for Cast expression. */
@Visitable
public class CastExpression extends Expression {
  @Visitable Expression expression;
  @Visitable TypeDescriptor castTypeDescriptor;

  private CastExpression(Expression expression, TypeDescriptor castTypeDescriptor) {
    this.expression = checkNotNull(expression);
    this.castTypeDescriptor = checkNotNull(castTypeDescriptor);
  }

  public TypeDescriptor getCastTypeDescriptor() {
    return castTypeDescriptor;
  }

  public Expression getExpression() {
    return expression;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return castTypeDescriptor;
  }

  @Override
  public Precedence getPrecedence() {
    return Precedence.CAST;
  }

  @Override
  public CastExpression clone() {
    return new CastExpression(expression.clone(), castTypeDescriptor);
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_CastExpression.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for CastExpressions. */
  public static class Builder {
    private Expression expression;
    private TypeDescriptor castTypeDescriptor;

    public static Builder from(CastExpression cast) {
      return new Builder()
          .setExpression(cast.getExpression())
          .setCastTypeDescriptor(cast.getCastTypeDescriptor());
    }

    public Builder setExpression(Expression expression) {
      this.expression = expression;
      return this;
    }

    public Builder setCastTypeDescriptor(TypeDescriptor castTypeDescriptor) {
      this.castTypeDescriptor = castTypeDescriptor;
      return this;
    }

    public CastExpression build() {
      return new CastExpression(AstUtils.removeJsDocCastIfPresent(expression), castTypeDescriptor);
    }
  }
}
