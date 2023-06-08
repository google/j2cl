/*
 * Copyright 2021 Google Inc.
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
import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Class for local class definitions. */
@Visitable
public class LocalClassDeclarationStatement extends Statement {
  @Visitable Type localClass;

  public LocalClassDeclarationStatement(Type localClass, SourcePosition sourcePosition) {
    super(sourcePosition);
    this.localClass = checkNotNull(localClass);
  }

  public Type getLocalClass() {
    return localClass;
  }

  @Override
  public LocalClassDeclarationStatement clone() {
    checkState(false);
    return null;
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_LocalClassDeclarationStatement.visit(processor, this);
  }
}
