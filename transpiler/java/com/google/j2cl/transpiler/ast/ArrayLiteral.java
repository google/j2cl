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
import static com.google.common.base.Preconditions.checkState;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class for array literal expressions.
 */
@Visitable
public class ArrayLiteral extends Expression {
  @Visitable ArrayTypeDescriptor typeDescriptor;
  @Visitable List<Expression> valueExpressions = new ArrayList<>();

  private ArrayLiteral(
      ArrayTypeDescriptor typeDescriptor, List<? extends Expression> valueExpressions) {
    checkState(typeDescriptor.isArray());

    this.typeDescriptor = typeDescriptor.toNonNullable();
    this.valueExpressions.addAll(checkNotNull(valueExpressions));
  }

  public List<Expression> getValueExpressions() {
    return valueExpressions;
  }

  @Override
  public ArrayTypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public Precedence getPrecedence() {
    // Array literals do no need parenthesis as they are already enclosed by square brackets.
    return Precedence.HIGHEST;
  }

  @Override
  public boolean canBeNull() {
    return false;
  }

  @Override
  public ArrayLiteral clone() {
    return new ArrayLiteral(typeDescriptor, AstUtils.clone(valueExpressions));
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_ArrayLiteral.visit(processor, this);
  }

  public Builder toBuilder() {
    return newBuilder()
        .setTypeDescriptor(getTypeDescriptor())
        .setValueExpressions(getValueExpressions());
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for ArrayLiteral. */
  public static class Builder {
    private ArrayTypeDescriptor typeDescriptor;
    private List<Expression> valueExpressions = new ArrayList<>();

    @CanIgnoreReturnValue
    public Builder setTypeDescriptor(ArrayTypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setValueExpressions(List<Expression> valueExpressions) {
      this.valueExpressions = new ArrayList<>(valueExpressions);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setValueExpressions(Expression... valueExpressions) {
      return setValueExpressions(Arrays.asList(valueExpressions));
    }

    @CanIgnoreReturnValue
    public Builder addValueExpressions(List<Expression> valueExpressions) {
      this.valueExpressions.addAll(valueExpressions);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder addValueExpressions(Expression... valueExpressions) {
      Collections.addAll(this.valueExpressions, valueExpressions);
      return this;
    }

    public ArrayLiteral build() {
      return new ArrayLiteral(typeDescriptor, valueExpressions);
    }
  }
}
