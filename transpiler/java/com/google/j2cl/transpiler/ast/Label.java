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

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Class for label. */
@Visitable
public class Label extends NameDeclaration implements Cloneable<Label> {
  private Label(String name) {
    super(name);
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_Label.visit(processor, this);
  }

  @Override
  public LabelReference createReference() {
    return new LabelReference(this);
  }

  @Override
  public Label clone() {
    return Label.Builder.from(this).build();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for Label. */
  public static class Builder {
    private String name;

    public static Builder from(Label variable) {
      Builder builder = new Builder();
      builder.name = variable.getName();
      return builder;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Label build() {
      checkState(name != null);
      return new Label(name);
    }
  }
}
