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
import com.google.common.collect.ImmutableList;

/**
 * A (by signature) reference to a method.
 */
@AutoValue
public abstract class MethodReference {
  public static MethodReference create(
      boolean isStatic,
      TypeReference enclosingClassReference,
      String methodName,
      TypeReference returnTypeReference,
      TypeReference... parameterTypeReferences) {
    return new AutoValue_MethodReference(
        isStatic,
        enclosingClassReference,
        methodName,
        ImmutableList.copyOf(parameterTypeReferences),
        returnTypeReference);
  }

  public abstract boolean isStatic();

  public abstract TypeReference getEnclosingClassReference();

  public abstract String getMethodName();

  public abstract ImmutableList<TypeReference> getParameterTypeReferences();

  public abstract TypeReference getReturnTypeReference();
}
