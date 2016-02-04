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

/**
 * A Builder for easily and correctly creating modified versions of fields.
 */
public class FieldBuilder {
  private FieldDescriptor fieldDescriptor;
  private Expression initializer;
  private boolean compileTimeConstant;
  private boolean isEnumField;
  private Variable capturedVariable;
  private Integer position;

  public static FieldBuilder from(Field field) {
    FieldBuilder builder = new FieldBuilder();
    builder.fieldDescriptor = field.getDescriptor();
    builder.initializer = field.getInitializer();
    builder.compileTimeConstant = field.isCompileTimeConstant();
    builder.isEnumField = field.isEnumField();
    builder.capturedVariable = field.getCapturedVariable();
    builder.position = field.getPosition();
    return builder;
  }

  public static FieldBuilder fromDefault(FieldDescriptor fieldDescriptor) {
    FieldBuilder builder = new FieldBuilder();
    builder.fieldDescriptor = fieldDescriptor;
    return builder;
  }

  public FieldBuilder initializer(Expression initializer) {
    this.initializer = initializer;
    return this;
  }

  public FieldBuilder compileTimeConstant(boolean compileTimeConstant) {
    this.compileTimeConstant = compileTimeConstant;
    return this;
  }

  public FieldBuilder isEnumField(boolean isEnumField) {
    this.isEnumField = isEnumField;
    return this;
  }

  public FieldBuilder capturedVariable(Variable capturedVariable) {
    this.capturedVariable = capturedVariable;
    return this;
  }

  public FieldBuilder enclosingClass(TypeDescriptor enclosingClassTypeDescriptor) {
    this.fieldDescriptor =
        FieldDescriptorBuilder.from(fieldDescriptor)
            .enclosingClass(enclosingClassTypeDescriptor)
            .build();
    return this;
  }

  public FieldBuilder setPosition(int position) {
    this.position = position;
    return this;
  }

  public Field build() {
    Preconditions.checkNotNull(position, "A position must be set");
    return new Field(
        fieldDescriptor, initializer, compileTimeConstant, isEnumField, capturedVariable, position);
  }
}
