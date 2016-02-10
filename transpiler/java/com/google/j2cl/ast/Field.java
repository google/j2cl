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

import com.google.common.base.Preconditions;
import com.google.j2cl.ast.processors.Context;
import com.google.j2cl.ast.processors.Visitable;

import javax.annotation.Nullable;

/**
 * Field declaration node.
 */
@Visitable
@Context
public class Field extends Node implements Positioned {
  @Visitable FieldDescriptor fieldDescriptor;
  @Visitable @Nullable Expression initializer;
  private boolean isEnumField;
  private Variable capturedVariable;
  private final int position;

  public Field(
      FieldDescriptor fieldDescriptor,
      Expression initializer,
      boolean isEnumField,
      Variable capturedVariable,
      int position) {
    this(fieldDescriptor, position);
    this.initializer = initializer;
    this.isEnumField = isEnumField;
    this.capturedVariable = capturedVariable;
  }

  public Field(FieldDescriptor fieldDescriptor, int position) {
    Preconditions.checkNotNull(fieldDescriptor);
    this.fieldDescriptor = fieldDescriptor;
    this.position = position;
  }

  public FieldDescriptor getDescriptor() {
    return fieldDescriptor;
  }

  public Expression getInitializer() {
    return initializer;
  }

  public Variable getCapturedVariable() {
    return this.capturedVariable;
  }

  public boolean hasInitializer() {
    return initializer != null;
  }

  public boolean isCompileTimeConstant() {
    return fieldDescriptor.isCompileTimeConstant();
  }

  public boolean isEnumField() {
    return isEnumField;
  }

  @Override
  public int getPosition() {
    return position;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_Field.visit(processor, this);
  }
}
