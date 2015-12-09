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

import com.google.auto.value.AutoValue;
import com.google.j2cl.ast.processors.Visitable;

/**
 * A (by signature) reference to a field.
 */
@AutoValue
@Visitable
public abstract class FieldDescriptor extends Node implements Member {
  public static FieldDescriptor create(
      boolean isStatic,
      boolean isRaw,
      Visibility visibility,
      TypeDescriptor enclosingClassTypeDescriptor,
      String fieldName,
      TypeDescriptor typeDescriptor) {
    return new AutoValue_FieldDescriptor(
        isStatic, isRaw, visibility, enclosingClassTypeDescriptor, fieldName, typeDescriptor);
  }

  /**
   * Creates a raw field reference.
   */
  public static FieldDescriptor createRaw(
      boolean isStatic,
      TypeDescriptor enclosingClassTypeDescriptor,
      String fieldName,
      TypeDescriptor typeDescriptor) {
    return new AutoValue_FieldDescriptor(
        isStatic, true, Visibility.PUBLIC, enclosingClassTypeDescriptor, fieldName, typeDescriptor);
  }

  @Override
  public abstract boolean isStatic();

  /**
   * Returns whether this is a Raw reference. Raw references are not mangled in the output and
   * thus can be used to describe reference to JS apis.
   */
  public abstract boolean isRaw();

  public abstract Visibility getVisibility();

  @Override
  public abstract TypeDescriptor getEnclosingClassTypeDescriptor();

  public abstract String getFieldName();

  public abstract TypeDescriptor getTypeDescriptor();

  public boolean isFieldDescriptorForCapturedVariables() {
    return getFieldName().startsWith(AstUtils.CAPTURES_PREFIX);
  }

  public boolean isFieldDescriptorForEnclosingInstance() {
    return getFieldName().startsWith(AstUtils.ENCLOSING_INSTANCE_NAME);
  }

  public boolean isFieldDescriptorForAllCaptures() {
    return isFieldDescriptorForCapturedVariables() || isFieldDescriptorForEnclosingInstance();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_FieldDescriptor.visit(processor, this);
  }
}
