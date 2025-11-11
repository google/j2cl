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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Represents multiple ordered expressions as a single compound expression.
 * <p>
 * They are executed in order and the value of the last one is returned.
 */
@Visitable
public class MultiExpression extends Expression {
  @Visitable List<Expression> expressions;

  private MultiExpression(List<Expression> expressions) {
    this.expressions = expressions;
  }

  public List<Expression> getExpressions() {
    return expressions;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return expressions.getLast().getTypeDescriptor();
  }

  @Override
  public TypeDescriptor getDeclaredTypeDescriptor() {
    return expressions.getLast().getDeclaredTypeDescriptor();
  }

  @Override
  public boolean isLValue() {
    return expressions.getLast().isLValue();
  }

  @Override
  public boolean isEffectivelyInvariant() {
    return expressions.stream().allMatch(Expression::isEffectivelyInvariant);
  }

  @Override
  public boolean hasSideEffects() {
    return expressions.stream().anyMatch(Expression::hasSideEffects);
  }

  @Override
  public boolean isCompileTimeConstant() {
    return expressions.stream().allMatch(Expression::isCompileTimeConstant);
  }

  @Override
  @Nullable
  public Literal getConstantValue() {
    if (!isCompileTimeConstant()) {
      return null;
    }
    return expressions.getLast().getConstantValue();
  }

  @Override
  public Expression.Precedence getPrecedence() {
    // MutliExpressions are emitted always with a parenthesis, so no need for extra.
    return Precedence.HIGHEST;
  }

  @Override
  public boolean isAlwaysNull() {
    return expressions.getLast().isAlwaysNull();
  }

  @Override
  public boolean canBeNull() {
    return expressions.getLast().canBeNull();
  }

  @Override
  public Expression clone() {
    return MultiExpression.newBuilder().setExpressions(AstUtils.clone(expressions)).build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_MultiExpression.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for MultiExpression. */
  public static class Builder {
    private List<Expression> expressions = new ArrayList<>();

    @CanIgnoreReturnValue
    public Builder setExpressions(Expression... expressions) {
      return setExpressions(Arrays.asList(expressions));
    }

    @CanIgnoreReturnValue
    public Builder setExpressions(Collection<Expression> expressions) {
      this.expressions = new ArrayList<>(expressions);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder addExpressions(Expression... expressions) {
      Collections.addAll(this.expressions, expressions);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder addExpressions(Collection<Expression> expressions) {
      this.expressions.addAll(expressions);
      return this;
    }

    public Expression build() {
      checkState(!expressions.isEmpty());
      if (expressions.size() == 1) {
        return Iterables.getOnlyElement(expressions);
      }
      return new MultiExpression(expressions);
    }
  }
}
