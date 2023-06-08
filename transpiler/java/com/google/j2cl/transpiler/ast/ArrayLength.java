/*
 * Copyright 2018 Google Inc.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/**
 * Class that represents access to the length property of an array.
 *
 * <p>This could almost be modeled as a property (field) access on an array expression, except that
 * there is no common super type for all arrays to use as the enclosing type.
 */
@Visitable
public class ArrayLength extends Expression {
  @Visitable Expression arrayExpression;

  private ArrayLength(Expression arrayExpression) {
    checkArgument(arrayExpression.getTypeDescriptor().toRawTypeDescriptor().isArray());

    this.arrayExpression = checkNotNull(arrayExpression);
  }

  public Expression getArrayExpression() {
    return arrayExpression;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return PrimitiveTypes.INT;
  }

  @Override
  public boolean isIdempotent() {
    return arrayExpression.isIdempotent();
  }

  @Override
  public Precedence getPrecedence() {
    return Precedence.MEMBER_ACCESS;
  }

  @Override
  public ArrayLength clone() {
    return ArrayLength.newBuilder().setArrayExpression(arrayExpression.clone()).build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_ArrayLength.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for ArrayLength. */
  public static class Builder {
    private Expression arrayExpression;

    public static Builder from(ArrayLength arrayLength) {
      return newBuilder().setArrayExpression(arrayLength.getArrayExpression());
    }

    public Builder setArrayExpression(Expression arrayExpression) {
      this.arrayExpression = arrayExpression;
      return this;
    }

    public ArrayLength build() {
      return new ArrayLength(arrayExpression);
    }
  }
}
