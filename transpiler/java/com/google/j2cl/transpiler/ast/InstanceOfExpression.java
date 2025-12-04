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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import javax.annotation.Nullable;

/** Class for instanceof Expression. */
@Visitable
public class InstanceOfExpression extends Expression implements HasSourcePosition {
  @Visitable Expression expression;
  @Visitable @Nullable TypeDescriptor testTypeDescriptor;
  @Visitable @Nullable Pattern pattern;
  private final SourcePosition sourcePosition;

  private InstanceOfExpression(
      SourcePosition sourcePosition,
      Expression expression,
      TypeDescriptor testTypeDescriptor,
      Pattern pattern) {
    // Either the pattern is null or the test type is null.
    checkArgument((pattern == null) == (testTypeDescriptor != null));
    checkArgument(
        pattern != null
            || testTypeDescriptor instanceof DeclaredTypeDescriptor
            || testTypeDescriptor instanceof ArrayTypeDescriptor);
    this.expression = checkNotNull(expression);
    this.testTypeDescriptor = testTypeDescriptor;
    this.sourcePosition = sourcePosition;
    this.pattern = pattern;
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

  public Pattern getPattern() {
    return pattern;
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
    return new InstanceOfExpression(
        sourcePosition, expression.clone(), testTypeDescriptor, AstUtils.clone(pattern));
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
    private Pattern pattern;
    private SourcePosition sourcePosition;

    public static Builder from(InstanceOfExpression instanceOfExpression) {
      return new Builder()
          .setExpression(instanceOfExpression.getExpression())
          .setTestTypeDescriptor(instanceOfExpression.getTestTypeDescriptor())
          .setPattern(instanceOfExpression.getPattern())
          .setSourcePosition(instanceOfExpression.getSourcePosition());
    }

    @CanIgnoreReturnValue
    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setExpression(Expression expression) {
      this.expression = expression;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setTestTypeDescriptor(TypeDescriptor testTypeDescriptor) {
      this.testTypeDescriptor = testTypeDescriptor;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setPattern(Pattern pattern) {
      this.pattern = pattern;
      return this;
    }

    public InstanceOfExpression build() {
      return new InstanceOfExpression(sourcePosition, expression, testTypeDescriptor, pattern);
    }
  }
}
