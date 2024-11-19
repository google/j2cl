/*
 * Copyright 2024 Google Inc.
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
import java.util.ArrayList;
import java.util.List;

/** Class for switch expression. */
@Visitable
public class SwitchExpression extends Expression implements SwitchConstruct<SwitchExpression> {
  @Visitable TypeDescriptor typeDescriptor;
  @Visitable Expression expression;
  @Visitable List<SwitchCase> cases = new ArrayList<>();
  private final SourcePosition sourcePosition;

  private SwitchExpression(
      SourcePosition sourcePosition,
      TypeDescriptor typeDescriptor,
      Expression expression,
      List<SwitchCase> cases) {
    this.typeDescriptor = checkNotNull(typeDescriptor);
    this.expression = checkNotNull(expression);
    this.cases.addAll(checkNotNull(cases));
    this.sourcePosition = checkNotNull(sourcePosition);
  }

  @Override
  public Expression getExpression() {
    return expression;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public List<SwitchCase> getCases() {
    return cases;
  }

  @Override
  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  @Override
  public Precedence getPrecedence() {
    // In the only backend where this expression reaches (KOTLIN), it never needs to be
    // parenthesized.
    return Precedence.HIGHEST;
  }

  @Override
  public SwitchExpression clone() {
    return SwitchExpression.newBuilder()
        .setTypeDescriptor(typeDescriptor)
        .setExpression(expression.clone())
        .setCases(AstUtils.clone(cases))
        .build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_SwitchExpression.visit(processor, this);
  }

  @Override
  public Builder toBuilder() {
    return Builder.from(this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for SwitchExpression. */
  public static class Builder implements SwitchConstruct.Builder<SwitchExpression> {
    private TypeDescriptor typeDescriptor;
    private Expression expression;
    private List<SwitchCase> switchCases = new ArrayList<>();
    private SourcePosition sourcePosition = SourcePosition.NONE;

    public static <T extends SwitchConstruct<T>> Builder from(SwitchConstruct<T> switchConstruct) {
      return newBuilder()
          .setExpression(switchConstruct.getExpression())
          .setCases(switchConstruct.getCases())
          .setTypeDescriptor(switchConstruct.getTypeDescriptor())
          .setSourcePosition(switchConstruct.getSourcePosition());
    }

    @Override
    @CanIgnoreReturnValue
    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setTypeDescriptor(TypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    @Override
    @CanIgnoreReturnValue
    public Builder setExpression(Expression expression) {
      this.expression = expression;
      return this;
    }

    @Override
    @CanIgnoreReturnValue
    public Builder setCases(List<SwitchCase> cases) {
      this.switchCases = new ArrayList<>(cases);
      return this;
    }

    @Override
    public SwitchExpression build() {
      return new SwitchExpression(sourcePosition, typeDescriptor, expression, switchCases);
    }

    private Builder() {}
  }
}
