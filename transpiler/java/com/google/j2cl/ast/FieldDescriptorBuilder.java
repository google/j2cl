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

/**
 * A Builder for easily and correctly creating modified versions of FieldDescriptors.
 */
public class FieldDescriptorBuilder {
  private boolean isStatic;
  private boolean isRaw;
  private Visibility visibility;
  private TypeDescriptor enclosingClassTypeDescriptor;
  private String fieldName;
  private TypeDescriptor typeDescriptor;

  public FieldDescriptorBuilder from(FieldDescriptor fieldDescriptor) {
    FieldDescriptorBuilder builder = new FieldDescriptorBuilder();
    builder.isStatic = fieldDescriptor.isStatic();
    builder.isRaw = fieldDescriptor.isRaw();
    builder.visibility = fieldDescriptor.getVisibility();
    builder.enclosingClassTypeDescriptor = fieldDescriptor.getEnclosingClassTypeDescriptor();
    builder.fieldName = fieldDescriptor.getFieldName();
    builder.typeDescriptor = fieldDescriptor.getTypeDescriptor();
    return builder;
  }

  public FieldDescriptorBuilder enclosingClass(TypeDescriptor enclosingClassTypeDescriptor) {
    this.enclosingClassTypeDescriptor = enclosingClassTypeDescriptor;
    return this;
  }

  public FieldDescriptor build() {
    return FieldDescriptor.create(
        isStatic, isRaw, visibility, enclosingClassTypeDescriptor, fieldName, typeDescriptor);
  }
}
