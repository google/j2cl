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

/** Continue Statement. */
@Visitable
public class ContinueStatement extends Statement {

  @Nullable @Visitable LabelReference labelReference;

  private ContinueStatement(SourcePosition sourcePosition, LabelReference labelReference) {
    super(sourcePosition);
    this.labelReference = labelReference;
  }

  public LabelReference getLabelReference() {
    return labelReference;
  }

  @Override
  public ContinueStatement clone() {
    return new ContinueStatement(getSourcePosition(), AstUtils.clone(labelReference));
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_ContinueStatement.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for ContinueStatement. */
  public static class Builder {
    private LabelReference labelReference;
    private SourcePosition sourcePosition;

    public static Builder from(ContinueStatement continueStatement) {
      return newBuilder()
          .setSourcePosition(continueStatement.getSourcePosition())
          .setLabelReference(continueStatement.getLabelReference());
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public Builder setLabelReference(LabelReference labelReference) {
      this.labelReference = labelReference;
      return this;
    }

    public ContinueStatement build() {
      return new ContinueStatement(sourcePosition, labelReference);
    }
  }
}
