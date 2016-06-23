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
import com.google.j2cl.ast.common.HasMetadata;
import com.google.j2cl.ast.sourcemap.HasSourcePosition;
import com.google.j2cl.ast.sourcemap.SourcePosition;

/**
 * A base class for Statement.
 */
@Visitable
public abstract class Statement extends Node implements HasSourcePosition {
  // unknown by default.
  private SourcePosition sourcePosition = SourcePosition.UNKNOWN;

  @Override
  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  @Override
  public void setSourcePosition(SourcePosition sourcePosition) {
    this.sourcePosition = sourcePosition;
  }

  @Override
  public void copyMetadataFrom(HasMetadata<HasSourcePosition> store) {
    setSourcePosition(store.getMetadata().getSourcePosition());
  }

  @Override
  public HasSourcePosition getMetadata() {
    return this;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_Statement.visit(processor, this);
  }
}
