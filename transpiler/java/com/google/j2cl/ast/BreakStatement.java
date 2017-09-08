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

import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.SourcePosition;
import javax.annotation.Nullable;

/**
 * Break Statement.
 */
@Visitable
public class BreakStatement extends Statement {

  @Nullable private final String labelName;

  public BreakStatement(SourcePosition sourcePosition, String labelName) {
    super(sourcePosition);
    this.labelName = labelName;
  }

  public String getLabelName() {
    return labelName;
  }

  @Override
  public BreakStatement clone() {
    return new BreakStatement(getSourcePosition(), labelName);
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_BreakStatement.visit(processor, this);
  }
}
