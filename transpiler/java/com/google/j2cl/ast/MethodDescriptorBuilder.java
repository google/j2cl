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

import com.google.common.collect.ImmutableList;

/**
 * A Builder for easily and correctly creating modified versions of MethodDescriptors.
 */
public class MethodDescriptorBuilder {
  private boolean isStatic;
  private boolean isRaw;
  private Visibility visibility;
  private TypeDescriptor enclosingClassTypeDescriptor;
  private String methodName;
  private boolean isConstructor;
  private boolean isNative;
  private ImmutableList<TypeDescriptor> parameterTypeDescriptors;
  private TypeDescriptor returnTypeDescriptor;
  private ImmutableList<TypeDescriptor> typeParameterDescriptors;

  public static MethodDescriptorBuilder from(MethodDescriptor methodDescriptor) {
    MethodDescriptorBuilder builder = new MethodDescriptorBuilder();
    builder.isStatic = methodDescriptor.isStatic();
    builder.isRaw = methodDescriptor.isRaw();
    builder.visibility = methodDescriptor.getVisibility();
    builder.enclosingClassTypeDescriptor = methodDescriptor.getEnclosingClassTypeDescriptor();
    builder.methodName = methodDescriptor.getMethodName();
    builder.isConstructor = methodDescriptor.isConstructor();
    builder.isNative = methodDescriptor.isNative();
    builder.parameterTypeDescriptors = methodDescriptor.getParameterTypeDescriptors();
    builder.returnTypeDescriptor = methodDescriptor.getReturnTypeDescriptor();
    builder.typeParameterDescriptors = methodDescriptor.getTypeParameterTypeDescriptors();
    return builder;
  }

  public MethodDescriptorBuilder enclosingClassTypeDescriptor(
      TypeDescriptor enclosingClassTypeDescriptor) {
    this.enclosingClassTypeDescriptor = enclosingClassTypeDescriptor;
    return this;
  }

  public MethodDescriptorBuilder returnTypeDescriptor(TypeDescriptor returnTypeDescriptor) {
    this.returnTypeDescriptor = returnTypeDescriptor;
    return this;
  }

  public MethodDescriptorBuilder parameterTypeDescriptors(
      Iterable<TypeDescriptor> parameterTypeDescriptors) {
    this.parameterTypeDescriptors = ImmutableList.copyOf(parameterTypeDescriptors);
    return this;
  }

  public MethodDescriptorBuilder isStatic(boolean isStatic) {
    this.isStatic = isStatic;
    return this;
  }

  public MethodDescriptor build() {
    return MethodDescriptor.create(
        isStatic,
        isRaw,
        visibility,
        enclosingClassTypeDescriptor,
        methodName,
        isConstructor,
        isNative,
        returnTypeDescriptor,
        parameterTypeDescriptors,
        typeParameterDescriptors);
  }
}
