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
public abstract class MethodDescriptor extends Node implements Member {
  public static final String METHOD_INIT = "$init";

  public static MethodDescriptor create(
      boolean isStatic,
      Visibility visibility,
      TypeDescriptor enclosingClassTypeDescriptor,
      String methodName,
      boolean isConstructor,
      TypeDescriptor returnTypeDescriptor,
      TypeDescriptor... parameterTypeDescriptors) {
    return new AutoValue_MethodDescriptor(
        isStatic,
        false,
        visibility,
        enclosingClassTypeDescriptor,
        methodName,
        isConstructor,
        ImmutableList.copyOf(parameterTypeDescriptors),
        returnTypeDescriptor);
  }

  /**
   * Creates a raw method reference.
   */
  public static MethodDescriptor createRaw(
      boolean isStatic, TypeDescriptor enclosingClassTypeDescriptor, String methodName) {
    return new AutoValue_MethodDescriptor(
        isStatic,
        true,
        Visibility.PUBLIC,
        enclosingClassTypeDescriptor,
        methodName,
        false,
        ImmutableList.<TypeDescriptor>of(),
        TypeDescriptor.VOID_TYPE_DESCRIPTOR);
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

  public abstract String getMethodName();

  public abstract boolean isConstructor();

  public abstract ImmutableList<TypeDescriptor> getParameterTypeDescriptors();

  public abstract TypeDescriptor getReturnTypeDescriptor();

  public boolean isInit() {
    return getMethodName().equals(METHOD_INIT);
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_MethodDescriptor.visit(processor, this);
  }
}
