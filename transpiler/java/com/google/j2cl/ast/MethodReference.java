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
import com.google.j2cl.ast.processors.Visitable;

/**
 * A (by signature) reference to a method.
 */
@AutoValue
@Visitable
public abstract class MethodReference extends Node implements Member {
  public static final String METHOD_INIT = "$init";

  public static MethodReference create(
      boolean isStatic,
      Visibility visibility,
      TypeReference enclosingClassRef,
      String methodName,
      boolean isConstructor,
      TypeReference returnTypeRef,
      TypeReference... parameterTypeRefs) {
    return new AutoValue_MethodReference(
        isStatic,
        false,
        visibility,
        enclosingClassRef,
        methodName,
        isConstructor,
        ImmutableList.copyOf(parameterTypeRefs),
        returnTypeRef);
  }

  /**
   * Creates a raw method reference.
   */
  public static MethodReference createRaw(
      boolean isStatic, TypeReference enclosingClassRef, String methodName) {
    return new AutoValue_MethodReference(
        isStatic,
        true,
        Visibility.PUBLIC,
        enclosingClassRef,
        methodName,
        false,
        ImmutableList.<TypeReference>of(),
        TypeReference.VOID_TYPEREF);
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
  public abstract TypeReference getEnclosingClassRef();

  public abstract String getMethodName();

  public abstract boolean isConstructor();

  public abstract ImmutableList<TypeReference> getParameterTypeRefs();

  public abstract TypeReference getReturnTypeRef();

  public boolean isInit() {
    return getMethodName().equals(METHOD_INIT);
  }

  @Override
  public MethodReference accept(Processor processor) {
    return Visitor_MethodReference.visit(processor, this);
  }
}
