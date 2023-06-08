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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import com.google.j2cl.transpiler.ast.Expression.Precedence;

/** Class for instanceof Expression. */
@Visitable
public class InstanceOfExpression extends Expression implements HasSourcePosition {
  @Visitable Expression expression;
  private final TypeDescriptor testTypeDescriptor;
  private final SourcePosition sourcePosition;

  private InstanceOfExpression(
      SourcePosition sourcePosition, Expression expression, TypeDescriptor testTypeDescriptor) {
    this.expression = checkNotNull(expression);
    this.testTypeDescriptor = checkNotNull(testTypeDescriptor);
    this.sourcePosition = sourcePosition;
    checkArgument(
        testTypeDescriptor instanceof DeclaredTypeDescriptor
            || testTypeDescriptor instanceof ArrayTypeDescriptor);
  }

  public Expression getExpression() {
    return expression;
  }

  public TypeDescriptor getTestTypeDescriptor() {
    return testTypeDescriptor;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return PrimitiveTypes.BOOLEAN;
  }

  @Override
  public boolean isIdempotent() {
    return expression.isIdempotent();
  }

  @Override
  public Precedence getPrecedence() {
    // instanceof is treated like a relational infix operator.
    return Precedence.RELATIONAL;
  }

  @Override
  public InstanceOfExpression clone() {
    return new InstanceOfExpression(sourcePosition, expression.clone(), testTypeDescriptor);
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_InstanceOfExpression.visit(processor, this);
  }

  @Override
  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for InstanceOfExpression. */
  public static class Builder {
    private Expression expression;
    private TypeDescriptor testTypeDescriptor;
    private SourcePosition sourcePosition;

    public static Builder from(InstanceOfExpression instanceOfExpression) {
      return new Builder()
          .setExpression(instanceOfExpression.getExpression())
          .setTestTypeDescriptor(instanceOfExpression.getTestTypeDescriptor());
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public Builder setExpression(Expression expression) {
      this.expression = expression;
      return this;
    }

    public Builder setTestTypeDescriptor(TypeDescriptor castTypeDescriptor) {
      this.testTypeDescriptor = castTypeDescriptor;
      return this;
    }

    public InstanceOfExpression build() {
      return new InstanceOfExpression(sourcePosition, expression, testTypeDescriptor);
    }
  }
}
