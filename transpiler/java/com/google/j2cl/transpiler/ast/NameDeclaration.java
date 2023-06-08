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

/** Class that represents constructs that declare a name, and that can be referenced. */
@Visitable
public abstract class NameDeclaration extends Node implements HasName {
  private final String name;

  NameDeclaration(String name) {
    this.name = checkNotNull(name);
  }

  @Override
  public String getName() {
    return name;
  }

  public abstract Reference<? extends NameDeclaration> createReference();

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_NameDeclaration.visit(processor, this);
  }
}
