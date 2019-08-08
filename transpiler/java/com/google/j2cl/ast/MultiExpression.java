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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Iterables;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.ast.processors.common.Processor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Represents multiple ordered expressions as a single compound expression.
 * <p>
 * They are executed in order and the value of the last one is returned.
 */
@Visitable
public class MultiExpression extends Expression {
  @Visitable List<Expression> expressions;

  private MultiExpression(List<Expression> expressions) {
    checkArgument(!expressions.isEmpty());
    this.expressions = expressions;
  }

  public List<Expression> getExpressions() {
    return expressions;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return Iterables.getLast(expressions).getTypeDescriptor();
  }

  @Override
  public TypeDescriptor getDeclaredTypeDescriptor() {
    return Iterables.getLast(expressions).getDeclaredTypeDescriptor();
  }

  @Override
  public boolean isLValue() {
    return Iterables.getLast(expressions).isLValue();
  }

  @Override
  public boolean isEffectivelyInvariant() {
    return expressions.stream().allMatch(Expression::isEffectivelyInvariant);
  }

  @Override
  public boolean hasSideEffects() {
    return expressions.stream().allMatch(Expression::hasSideEffects);
  }

  @Override
  public boolean isCompileTimeConstant() {
    return expressions.stream().allMatch(Expression::isCompileTimeConstant);
  }

  @Override
  public MultiExpression clone() {
    return MultiExpression.newBuilder().setExpressions(AstUtils.clone(expressions)).build();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_MultiExpression.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for MultiExpression. */
  public static class Builder {
    private List<Expression> expressions = new ArrayList<>();

    public Builder setExpressions(Expression... expressions) {
      return setExpressions(Arrays.asList(expressions));
    }

    public Builder setExpressions(Collection<Expression> expressions) {
      this.expressions = new ArrayList<>(expressions);
      return this;
    }

    public Builder addExpressions(Expression... expressions) {
      this.expressions.addAll(Arrays.asList(expressions));
      return this;
    }

    public Builder addExpressions(Collection<Expression> expressions) {
      this.expressions.addAll(expressions);
      return this;
    }

    public MultiExpression build() {
      return new MultiExpression(expressions);
    }
  }
}
