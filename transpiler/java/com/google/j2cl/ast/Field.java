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
 * Field definition node.
 */
public class Field extends Variable {
  private Visibility visibility;
  private TypeReference declaringType;

  public Field(
      String name,
      TypeReference type,
      boolean isFinal,
      Expression initializer,
      Visibility visibility,
      TypeReference declaringType) {
    super(name, type, isFinal, initializer);
    this.visibility = visibility;
    this.declaringType = declaringType;
  }

  public Visibility getVisibility() {
    return visibility;
  }

  public void setVisibility(Visibility visibility) {
    this.visibility = visibility;
  }

  public TypeReference getDeclaringType() {
    return declaringType;
  }

  public void setDeclaringType(TypeReference declaringType) {
    this.declaringType = declaringType;
  }
}
