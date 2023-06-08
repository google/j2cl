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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** A base class for Statement. */
@Visitable
public abstract class Statement extends Node implements HasSourcePosition, Cloneable<Statement> {
  // unknown by default.
  private SourcePosition sourcePosition;

  public Statement(SourcePosition sourcePosition) {
    setSourcePosition(sourcePosition);
  }

  @Override
  public final SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  public final void setSourcePosition(SourcePosition sourcePosition) {
    this.sourcePosition = checkNotNull(sourcePosition);
  }

  public boolean isNoop() {
    return false;
  }

  public LabeledStatement encloseWithLabel(Label label) {
    return LabeledStatement.newBuilder()
        .setStatement(this)
        .setLabel(label)
        .setSourcePosition(getSourcePosition())
        .build();
  }

  @Override
  public abstract Statement clone();

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_Statement.visit(processor, this);
  }

  public static Statement createNoopStatement() {
    return Block.newBuilder().build();
  }
}
