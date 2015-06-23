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

import com.google.j2cl.ast.processors.Visitable;

import javax.annotation.Nullable;

/**
 * Field declaration node.
 */
@Visitable
public class Field extends Node {
  @Visitable FieldReference selfReference;
  @Visitable @Nullable Expression initializer;

  public Field(FieldReference selfReference, Expression initializer) {
    this.selfReference = selfReference;
    this.initializer = initializer;
  }

  public FieldReference getSelfReference() {
    return selfReference;
  }

  public void setSelfReference(FieldReference selfReference) {
    this.selfReference = selfReference;
  }

  public Expression getInitializer() {
    return initializer;
  }

  public void setInitializer(Expression initializer) {
    this.initializer = initializer;
  }

  public boolean hasInitializer() {
    return this.initializer != null;
  }

  @Override
  public Field accept(Processor processor) {
    return Visitor_Field.visit(processor, this);
  }
}
