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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Class for matching an expression against a pattern. */
@Visitable
public class PatternMatchExpression extends Expression implements HasSourcePosition {
  @Visitable Expression expression;
  @Visitable Pattern pattern;
  private final SourcePosition sourcePosition;

  private PatternMatchExpression(
      SourcePosition sourcePosition, Expression expression, Pattern pattern) {
    this.sourcePosition = sourcePosition;
    this.expression = checkNotNull(expression);
    this.pattern = checkNotNull(pattern);
  }

  public Expression getExpression() {
    return expression;
  }

  public Pattern getPattern() {
    return pattern;
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
  public PatternMatchExpression clone() {
    return new PatternMatchExpression(sourcePosition, expression.clone(), AstUtils.clone(pattern));
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_PatternMatchExpression.visit(processor, this);
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
    private Pattern pattern;
    private SourcePosition sourcePosition;

    public static Builder from(PatternMatchExpression patternMatchExpression) {
      return new Builder()
          .setExpression(patternMatchExpression.getExpression())
          .setPattern(patternMatchExpression.getPattern())
          .setSourcePosition(patternMatchExpression.getSourcePosition());
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
    public Builder setPattern(Pattern pattern) {
      this.pattern = pattern;
      return this;
    }

    public PatternMatchExpression build() {
      return new PatternMatchExpression(sourcePosition, expression, pattern);
    }
  }
}
