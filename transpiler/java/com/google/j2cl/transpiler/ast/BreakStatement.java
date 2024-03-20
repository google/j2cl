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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Break Statement. */
@Visitable
public class BreakStatement extends BreakOrContinueStatement {
  private BreakStatement(SourcePosition sourcePosition, LabelReference labelReference) {
    super(sourcePosition, labelReference);
  }

  @Override
  public BreakStatement clone() {
    return new BreakStatement(getSourcePosition(), AstUtils.clone(labelReference));
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_BreakStatement.visit(processor, this);
  }

  @Override
  public Builder toBuilder() {
    return Builder.from(this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for BreakStatement. */
  public static class Builder
      extends BreakOrContinueStatement.Builder<BreakStatement, BreakStatement.Builder> {
    private LabelReference labelReference;
    private SourcePosition sourcePosition;

    public static Builder from(BreakOrContinueStatement breakStatement) {
      return newBuilder()
          .setSourcePosition(breakStatement.getSourcePosition())
          .setLabelReference(breakStatement.getLabelReference());
    }

    @Override
    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    @Override
    public Builder setLabelReference(LabelReference labelReference) {
      this.labelReference = labelReference;
      return this;
    }

    @Override
    public BreakStatement build() {
      return new BreakStatement(sourcePosition, labelReference);
    }
  }
}
