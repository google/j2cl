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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Visitable;
import javax.annotation.Nullable;

/** Break or Continue Statement. */
@Visitable
public abstract class BreakOrContinueStatement extends Statement {

  @Nullable @Visitable LabelReference labelReference;

  BreakOrContinueStatement(SourcePosition sourcePosition, LabelReference labelReference) {
    super(sourcePosition);
    this.labelReference = labelReference;
  }

  public final LabelReference getLabelReference() {
    return labelReference;
  }

  public boolean targetsLabel(Label label) {
    return labelReference != null && labelReference.getTarget() == label;
  }

  public abstract <S extends BreakOrContinueStatement, B extends Builder<S, B>>
      Builder<S, B> toBuilder();

  /** Abstract builder class for BreakOrContinueStatement. */
  public abstract static class Builder<
      S extends BreakOrContinueStatement, B extends Builder<S, B>> {

    @CanIgnoreReturnValue
    public abstract B setLabelReference(LabelReference labelReference);

    @CanIgnoreReturnValue
    public abstract B setSourcePosition(SourcePosition sourcePosition);

    public abstract S build();
  }
}
