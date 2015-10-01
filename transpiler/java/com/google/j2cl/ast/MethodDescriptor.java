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
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.j2cl.ast.processors.Visitable;

import java.util.Arrays;
import java.util.List;

/**
 * A (by signature) reference to a method.
 */
@AutoValue
@Visitable
public abstract class MethodDescriptor extends Node implements Member {
  public static final String INIT_METHOD_NAME = "$init";
  public static final String VALUE_OF_METHOD_NAME = "valueOf"; // Boxed type valueOf() method.
  public static final String VALUE_METHOD_SUFFIX = "Value"; // Boxed type **Value() method.
  public static final String GET_ONE_METHOD_NAME = "$getOne"; // LongUtils.$getOne() method.

  private static Interner<MethodDescriptor> interner;

  public static MethodDescriptor create(
      boolean isStatic,
      boolean isRaw,
      Visibility visibility,
      TypeDescriptor enclosingClassTypeDescriptor,
      String methodName,
      boolean isConstructor,
      boolean isNative,
      TypeDescriptor returnTypeDescriptor,
      Iterable<TypeDescriptor> parameterTypeDescriptors,
      Iterable<TypeDescriptor> typeParameterDescriptors) {
    return getInterner()
        .intern(
            new AutoValue_MethodDescriptor(
                isStatic,
                isRaw,
                visibility,
                enclosingClassTypeDescriptor,
                methodName,
                isConstructor,
                isNative,
                ImmutableList.copyOf(parameterTypeDescriptors),
                returnTypeDescriptor,
                ImmutableList.copyOf(typeParameterDescriptors)));
  }

  public static MethodDescriptor create(
      boolean isStatic,
      Visibility visibility,
      TypeDescriptor enclosingClassTypeDescriptor,
      String methodName,
      boolean isConstructor,
      boolean isNative,
      TypeDescriptor returnTypeDescriptor,
      Iterable<TypeDescriptor> parameterTypeDescriptors,
      Iterable<TypeDescriptor> typeParameterDescriptors) {
    return create(
        isStatic,
        false,
        visibility,
        enclosingClassTypeDescriptor,
        methodName,
        isConstructor,
        isNative,
        returnTypeDescriptor,
        parameterTypeDescriptors,
        typeParameterDescriptors);
  }

  public static MethodDescriptor create(
      boolean isStatic,
      Visibility visibility,
      TypeDescriptor enclosingClassTypeDescriptor,
      String methodName,
      boolean isConstructor,
      boolean isNative,
      TypeDescriptor returnTypeDescriptor,
      Iterable<TypeDescriptor> parameterTypeDescriptors) {
    return create(
        isStatic,
        false,
        visibility,
        enclosingClassTypeDescriptor,
        methodName,
        isConstructor,
        isNative,
        returnTypeDescriptor,
        parameterTypeDescriptors,
        ImmutableList.<TypeDescriptor>of());
  }

  public static MethodDescriptor create(
      boolean isStatic,
      Visibility visibility,
      TypeDescriptor enclosingClassDescriptor,
      String methodName,
      boolean isConstructor,
      boolean isNative,
      TypeDescriptor returnTypeDescriptor,
      TypeDescriptor... parameterTypeDescriptors) {
    return create(
        isStatic,
        visibility,
        enclosingClassDescriptor,
        methodName,
        isConstructor,
        isNative,
        returnTypeDescriptor,
        Arrays.asList(parameterTypeDescriptors));
  }

  /**
   * Creates a raw method reference.
   */
  public static MethodDescriptor createRaw(
      boolean isStatic,
      Visibility visibility,
      TypeDescriptor enclosingClassTypeDescriptor,
      String methodName,
      List<TypeDescriptor> parameterTypeDescriptors,
      TypeDescriptor returnTypeDescriptor) {
    return create(
        isStatic,
        true,
        visibility,
        enclosingClassTypeDescriptor,
        methodName,
        false,
        false,
        returnTypeDescriptor,
        parameterTypeDescriptors,
        ImmutableList.<TypeDescriptor>of());
  }

  static Interner<MethodDescriptor> getInterner() {
    if (interner == null) {
      interner = Interners.newWeakInterner();
    }
    return interner;
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

  public abstract boolean isNative();

  public abstract ImmutableList<TypeDescriptor> getParameterTypeDescriptors();

  public abstract TypeDescriptor getReturnTypeDescriptor();

  /**
   * Type parameters declared in the method.
   */
  public abstract ImmutableList<TypeDescriptor> getTypeParameterDescriptors();

  public boolean isInit() {
    return getMethodName().equals(INIT_METHOD_NAME);
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_MethodDescriptor.visit(processor, this);
  }
}
