/*
 * Copyright 2025 Google Inc.
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
import javax.annotation.Nullable;

/** Class for Yield expression. */
@Visitable
public class YieldExpression extends Expression {
  @Visitable @Nullable Expression expression;
  @Visitable TypeDescriptor typeDescriptor;

  private YieldExpression(Expression expression, TypeDescriptor typeDescriptor) {
    this.expression = expression;
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
    // Yield is considered exactly like a assignment w.r.t. precedence.
    return Precedence.ASSIGNMENT;
  }

  @Override
  public YieldExpression clone() {
    return Builder.from(this).build();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_YieldExpression.visit(processor, this);
  }

  /** A builder for YieldExpressions. */
  public static class Builder {
    private Expression expression;
    private TypeDescriptor typeDescriptor;

    public static Builder from(YieldExpression yieldExpression) {
      return new Builder()
          .setExpression(yieldExpression.getExpression())
          .setTypeDescriptor(yieldExpression.getTypeDescriptor());
    }

    @CanIgnoreReturnValue
    public Builder setExpression(Expression expression) {
      this.expression = expression;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setTypeDescriptor(TypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    public YieldExpression build() {
      return new YieldExpression(expression, typeDescriptor);
    }
  }
}
