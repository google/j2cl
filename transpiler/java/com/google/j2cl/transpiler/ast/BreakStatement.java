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
import javax.annotation.Nullable;

/** Break Statement. */
@Visitable
public class BreakStatement extends Statement {

  @Nullable @Visitable LabelReference labelReference;

  private BreakStatement(SourcePosition sourcePosition, LabelReference labelReference) {
    super(sourcePosition);
    this.labelReference = labelReference;
  }

  public LabelReference getLabelReference() {
    return labelReference;
  }

  @Override
  public BreakStatement clone() {
    return new BreakStatement(getSourcePosition(), AstUtils.clone(labelReference));
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_BreakStatement.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for BreakStatement. */
  public static class Builder {
    private LabelReference labelReference;
    private SourcePosition sourcePosition;

    public static Builder from(BreakStatement breakStatement) {
      return newBuilder()
          .setSourcePosition(breakStatement.getSourcePosition())
          .setLabelReference(breakStatement.getLabelReference());
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public Builder setLabelReference(LabelReference labelReference) {
      this.labelReference = labelReference;
      return this;
    }

    public BreakStatement build() {
      return new BreakStatement(sourcePosition, labelReference);
    }
  }
}
