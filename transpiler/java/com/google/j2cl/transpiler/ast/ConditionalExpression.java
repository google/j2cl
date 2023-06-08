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

/** Ternary conditional expression. */
@Visitable
public class ConditionalExpression extends Expression {
  private final TypeDescriptor typeDescriptor;
  @Visitable Expression conditionExpression;
  @Visitable Expression trueExpression;
  @Visitable Expression falseExpression;

  private ConditionalExpression(
      TypeDescriptor typeDescriptor,
      Expression conditionExpression,
      Expression trueExpression,
      Expression falseExpression) {
    this.typeDescriptor =
        (!trueExpression.getTypeDescriptor().isNullable()
                && !falseExpression.getTypeDescriptor().isNullable())
            ? typeDescriptor.toNonNullable()
            : checkNotNull(typeDescriptor);
    this.conditionExpression = checkNotNull(conditionExpression);
    this.trueExpression = checkNotNull(trueExpression);
    this.falseExpression = checkNotNull(falseExpression);
  }

  public Expression getConditionExpression() {
    return conditionExpression;
  }

  public Expression getTrueExpression() {
    return trueExpression;
  }

  public Expression getFalseExpression() {
    return falseExpression;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public Expression.Precedence getPrecedence() {
    return Precedence.CONDITIONAL;
  }

  @Override
  public ConditionalExpression clone() {
    return new ConditionalExpression(
        typeDescriptor,
        conditionExpression.clone(),
        trueExpression.clone(),
        falseExpression.clone());
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_ConditionalExpression.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for ConditionalExpression. */
  public static class Builder {
    private TypeDescriptor typeDescriptor;
    private Expression conditionExpression;
    private Expression trueExpression;
    private Expression falseExpression;

    public static Builder from(ConditionalExpression conditionalExpression) {
      return new Builder()
          .setTypeDescriptor(conditionalExpression.getTypeDescriptor())
          .setConditionExpression(conditionalExpression.getConditionExpression())
          .setTrueExpression(conditionalExpression.getTrueExpression())
          .setFalseExpression(conditionalExpression.getFalseExpression());
    }

    public Builder setConditionExpression(Expression conditionExpression) {
      this.conditionExpression = conditionExpression;
      return this;
    }

    public Builder setTrueExpression(Expression trueExpression) {
      this.trueExpression = trueExpression;
      return this;
    }

    public Builder setFalseExpression(Expression falseExpression) {
      this.falseExpression = falseExpression;
      return this;
    }

    public Builder setTypeDescriptor(TypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    public ConditionalExpression build() {
      return new ConditionalExpression(
          typeDescriptor, conditionExpression, trueExpression, falseExpression);
    }
  }
}
