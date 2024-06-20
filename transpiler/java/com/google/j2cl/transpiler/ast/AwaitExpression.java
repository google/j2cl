/*
 * Copyright 2017 Google Inc.
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

/** Class for Await expression. */
@Visitable
public class AwaitExpression extends Expression {
  @Visitable Expression expression;
  @Visitable TypeDescriptor typeDescriptor;

  private AwaitExpression(Expression expression, TypeDescriptor typeDescriptor) {
    this.expression = checkNotNull(expression);
    this.typeDescriptor = checkNotNull(typeDescriptor);
  }

  public Expression getExpression() {
    return expression;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public Precedence getPrecedence() {
    // Await is considered exactly like a prefix operator w.r.t. precedence.
    return Precedence.PREFIX;
  }

  @Override
  public AwaitExpression clone() {
    return new AwaitExpression(expression.clone(), typeDescriptor);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_AwaitExpression.visit(processor, this);
  }

  /** A builder for AwaitExpressions. */
  public static class Builder {
    private Expression expression;
    private TypeDescriptor typeDescriptor;

    public static Builder from(AwaitExpression awaitExpression) {
      return new Builder()
          .setExpression(awaitExpression.getExpression())
          .setTypeDescriptor(awaitExpression.getTypeDescriptor());
    }

    public Builder setExpression(Expression expression) {
      this.expression = expression;
      return this;
    }

    public Builder setTypeDescriptor(TypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    public AwaitExpression build() {
      return new AwaitExpression(expression, typeDescriptor);
    }
  }
}
