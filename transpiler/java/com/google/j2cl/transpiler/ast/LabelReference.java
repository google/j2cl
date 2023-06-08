/*
 * Copyright 2020 Google Inc.
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

import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Class for a reference to a label. */
@Visitable
public class LabelReference extends Node implements Cloneable<LabelReference>, Reference<Label> {
  // Do not mark the target as @Visitable, LabelReference acts as an opaque reference from
  // the visitor perspective.
  private final Label label;

  LabelReference(Label label) {
    this.label = checkNotNull(label);
  }

  @Override
  public Label getTarget() {
    return label;
  }

  @Override
  public LabelReference clone() {
    return new LabelReference(label);
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_LabelReference.visit(processor, this);
  }
}
