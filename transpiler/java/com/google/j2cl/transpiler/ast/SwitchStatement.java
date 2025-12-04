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

import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Switch Statement. */
@Visitable
public class SwitchStatement extends Statement implements SwitchConstruct<SwitchStatement> {
  @Visitable Expression expression;
  @Visitable List<SwitchCase> cases = new ArrayList<>();

  private final boolean allowsNulls;

  private SwitchStatement(
      SourcePosition sourcePosition,
      Expression expression,
      List<SwitchCase> cases,
      boolean allowsNulls) {
    super(sourcePosition);
    this.expression = checkNotNull(expression);
    this.cases.addAll(checkNotNull(cases));
    this.allowsNulls = allowsNulls;
  }

  @Override
  public Expression getExpression() {
    return expression;
  }

  @Override
  public List<SwitchCase> getCases() {
    return cases;
  }

  @Override
  public boolean allowsNulls() {
    return allowsNulls;
  }

  /** Returns the position of the default case, -1 if there is no default case. */
  public int getDefaultCasePosition() {
    return Iterables.indexOf(getCases(), SwitchCase::isDefault);
  }

  @Override
  public SwitchStatement clone() {
    return SwitchStatement.newBuilder()
        .setSourcePosition(getSourcePosition())
        .setExpression(expression.clone())
        .setCases(AstUtils.clone(cases))
        .setAllowsNulls(allowsNulls)
        .build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_SwitchStatement.visit(processor, this);
  }

  @Override
  public Builder toBuilder() {
    return Builder.from(this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for SwitchStatement. */
  public static class Builder implements SwitchConstruct.Builder<SwitchStatement> {
    private Expression expression;
    private List<SwitchCase> switchCases = new ArrayList<>();

    private boolean allowsNulls;
    private SourcePosition sourcePosition;

    public static <T extends SwitchConstruct<T>> Builder from(SwitchConstruct<T> switchConstruct) {
      return newBuilder()
          .setSourcePosition(switchConstruct.getSourcePosition())
          .setExpression(switchConstruct.getExpression())
          .setCases(switchConstruct.getCases())
          .setAllowsNulls(switchConstruct.allowsNulls());
    }

    @Override
    @CanIgnoreReturnValue
    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
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

    @CanIgnoreReturnValue
    public Builder addCases(SwitchCase... cases) {
      Collections.addAll(switchCases, cases);
      return this;
    }

    @Override
    @CanIgnoreReturnValue
    public Builder setAllowsNulls(boolean allowsNulls) {
      this.allowsNulls = allowsNulls;
      return this;
    }

    @Override
    public SwitchStatement build() {
      return new SwitchStatement(sourcePosition, expression, switchCases, allowsNulls);
    }

    private Builder() {}
  }
}
