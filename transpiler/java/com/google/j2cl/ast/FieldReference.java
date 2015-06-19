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
public abstract class FieldReference extends Node {
  public static FieldReference create(
      boolean isStatic,
      Visibility visibility,
      TypeReference enclosingClassReference,
      String fieldName,
      TypeReference type) {
    return new AutoValue_FieldReference(
        isStatic, visibility, enclosingClassReference, fieldName, type);
  }

  public abstract boolean isStatic();

  public abstract Visibility getVisibility();

  public abstract TypeReference getEnclosingClassReference();

  public abstract String getFieldName();

  public abstract TypeReference getType();

  @Override
  FieldReference accept(Visitor visitor) {
    return Visitor_FieldReference.visit(visitor, this);
  }
}
