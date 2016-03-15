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
  private Visibility visibility;
  private TypeDescriptor enclosingClassTypeDescriptor;
  private String methodName;
  private boolean isConstructor;
  private boolean isNative;
  private boolean isVarargs;
  private ImmutableList<TypeDescriptor> parameterTypeDescriptors;
  private TypeDescriptor returnTypeDescriptor;
  private ImmutableList<TypeDescriptor> typeParameterDescriptors;
  private JsInfo jsInfo;

  public static MethodDescriptorBuilder fromDefault() {
    MethodDescriptorBuilder builder = new MethodDescriptorBuilder();
    builder.visibility = Visibility.PUBLIC;
    builder.enclosingClassTypeDescriptor =
        TypeDescriptors.BootstrapType.NATIVE_UTIL.getDescriptor();
    builder.methodName = "";
    builder.parameterTypeDescriptors = ImmutableList.<TypeDescriptor>of();
    builder.returnTypeDescriptor = TypeDescriptors.get().primitiveVoid;
    builder.typeParameterDescriptors = ImmutableList.<TypeDescriptor>of();
    builder.jsInfo = JsInfo.NONE;
    return builder;
  }

  public static MethodDescriptorBuilder from(MethodDescriptor methodDescriptor) {
    MethodDescriptorBuilder builder = new MethodDescriptorBuilder();
    builder.isStatic = methodDescriptor.isStatic();
    builder.visibility = methodDescriptor.getVisibility();
    builder.enclosingClassTypeDescriptor = methodDescriptor.getEnclosingClassTypeDescriptor();
    builder.methodName = methodDescriptor.getMethodName();
    builder.isConstructor = methodDescriptor.isConstructor();
    builder.isNative = methodDescriptor.isNative();
    builder.isVarargs = methodDescriptor.isVarargs();
    builder.parameterTypeDescriptors = methodDescriptor.getParameterTypeDescriptors();
    builder.returnTypeDescriptor = methodDescriptor.getReturnTypeDescriptor();
    builder.typeParameterDescriptors = methodDescriptor.getTypeParameterTypeDescriptors();
    builder.jsInfo = methodDescriptor.getJsInfo();
    return builder;
  }

  public MethodDescriptorBuilder enclosingClassTypeDescriptor(
      TypeDescriptor enclosingClassTypeDescriptor) {
    this.enclosingClassTypeDescriptor = enclosingClassTypeDescriptor;
    return this;
  }

  public MethodDescriptorBuilder methodName(String methodName) {
    this.methodName = methodName;
    return this;
  }

  public MethodDescriptorBuilder returnTypeDescriptor(TypeDescriptor returnTypeDescriptor) {
    this.returnTypeDescriptor = returnTypeDescriptor;
    return this;
  }

  public MethodDescriptorBuilder parameterTypeDescriptors(TypeDescriptor... typeDescriptors) {
    this.parameterTypeDescriptors = ImmutableList.copyOf(typeDescriptors);
    return this;
  }

  public MethodDescriptorBuilder parameterTypeDescriptors(
      Iterable<TypeDescriptor> parameterTypeDescriptors) {
    this.parameterTypeDescriptors = ImmutableList.copyOf(parameterTypeDescriptors);
    return this;
  }

  public MethodDescriptorBuilder isConstructor(boolean isConstructor) {
    this.isConstructor = isConstructor;
    return this;
  }

  public MethodDescriptorBuilder isStatic(boolean isStatic) {
    this.isStatic = isStatic;
    return this;
  }

  public MethodDescriptorBuilder typeParameterDescriptors(
      Iterable<TypeDescriptor> typeParameterDescriptors) {
    this.typeParameterDescriptors = ImmutableList.copyOf(typeParameterDescriptors);
    return this;
  }

  public MethodDescriptorBuilder visibility(Visibility visibility) {
    this.visibility = visibility;
    return this;
  }

  public MethodDescriptorBuilder jsInfo(JsInfo jsInfo) {
    this.jsInfo = jsInfo;
    return this;
  }

  public MethodDescriptor build() {
    return MethodDescriptor.create(
        isStatic,
        visibility,
        enclosingClassTypeDescriptor,
        methodName,
        isConstructor,
        isNative,
        isVarargs,
        returnTypeDescriptor,
        parameterTypeDescriptors,
        typeParameterDescriptors,
        jsInfo);
  }
}
