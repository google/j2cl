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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Switch Statement. */
@Visitable
public class SwitchStatement extends Statement {
  @Visitable Expression switchExpression;
  @Visitable List<SwitchCase> cases = new ArrayList<>();

  private SwitchStatement(
      SourcePosition sourcePosition, Expression switchExpression, List<SwitchCase> cases) {
    super(sourcePosition);
    this.switchExpression = checkNotNull(switchExpression);
    this.cases.addAll(checkNotNull(cases));
  }

  public Expression getSwitchExpression() {
    return switchExpression;
  }

  public List<SwitchCase> getCases() {
    return cases;
  }

  /** Returns the position of the default case, -1 if there is no default case. */
  public int getDefaultCasePosition() {
    return Iterables.indexOf(getCases(), SwitchCase::isDefault);
  }

  @Override
  public SwitchStatement clone() {
    return SwitchStatement.newBuilder()
        .setSourcePosition(getSourcePosition())
        .setSwitchExpression(switchExpression.clone())
        .setCases(AstUtils.clone(cases))
        .build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_SwitchStatement.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for SwitchStatement. */
  public static class Builder {
    private Expression switchExpression;
    private List<SwitchCase> switchCases = new ArrayList<>();
    private SourcePosition sourcePosition;

    public static Builder from(SwitchStatement switchStatement) {
      return newBuilder()
          .setSourcePosition(switchStatement.getSourcePosition())
          .setSwitchExpression(switchStatement.getSwitchExpression())
          .setCases(switchStatement.getCases());
    }

    @CanIgnoreReturnValue
    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setSwitchExpression(Expression switchExpression) {
      this.switchExpression = switchExpression;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setCases(SwitchCase... cases) {
      return setCases(Arrays.asList(cases));
    }

    @CanIgnoreReturnValue
    public Builder setCases(Collection<SwitchCase> cases) {
      this.switchCases = new ArrayList<>(cases);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder addCases(SwitchCase... cases) {
      Collections.addAll(switchCases, cases);
      return this;
    }

    public SwitchStatement build() {
      return new SwitchStatement(sourcePosition, switchExpression, switchCases);
    }

    private Builder() {}
  }
}
