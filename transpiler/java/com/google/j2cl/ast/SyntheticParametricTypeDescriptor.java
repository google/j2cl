/*
 * Copyright 2016 Google Inc.
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
 * Represents a synthetic parametric TypeDescriptor.
 *
 * <p>A SyntheticParametricTypeDescriptor wraps the original RegularTypeDescriptor, with a given
 * type arguments list. It returns consistent result as its original type.
 */
public class SyntheticParametricTypeDescriptor extends RegularTypeDescriptor {
  private RegularTypeDescriptor originalTypeDescriptor;

  SyntheticParametricTypeDescriptor(
      RegularTypeDescriptor originalTypeDescriptor,
      Iterable<TypeDescriptor> typeArgumentTypeDescriptors) {
    super(null);
    this.originalTypeDescriptor = originalTypeDescriptor;
    this.packageComponents = originalTypeDescriptor.getPackageComponents();
    this.classComponents = originalTypeDescriptor.getClassComponents();
    this.isRaw = originalTypeDescriptor.isRaw();
    this.typeArgumentDescriptors = ImmutableList.copyOf(typeArgumentTypeDescriptors);
    this.isJsFunction = originalTypeDescriptor.isJsFunctionInterface();
    this.isJsType = originalTypeDescriptor.isJsType();
    this.isNative = originalTypeDescriptor.isNative();
    this.jsTypeNamespace = originalTypeDescriptor.getJsNamespace();
    this.jsTypeName = originalTypeDescriptor.getJsName();
  }

  @Override
  public TypeDescriptor getRawTypeDescriptor() {
    return TypeDescriptor.createSyntheticParametricTypeDescriptor(
        originalTypeDescriptor, ImmutableList.<TypeDescriptor>of());
  }

  @Override
  public TypeDescriptor getSuperTypeDescriptor() {
    return originalTypeDescriptor.getSuperTypeDescriptor();
  }

  @Override
  public TypeDescriptor getEnclosingTypeDescriptor() {
    return originalTypeDescriptor.getEnclosingTypeDescriptor();
  }

  @Override
  public Visibility getVisibility() {
    return originalTypeDescriptor.getVisibility();
  }

  @Override
  public boolean isInstanceMemberClass() {
    return originalTypeDescriptor.isInstanceMemberClass();
  }

  @Override
  public boolean isInstanceNestedClass() {
    return originalTypeDescriptor.isInstanceNestedClass();
  }

  @Override
  public boolean isInterface() {
    return originalTypeDescriptor.isInterface();
  }

  @Override
  public boolean isEnumOrSubclass() {
    return originalTypeDescriptor.isEnumOrSubclass();
  }

  @Override
  public boolean isJsFunctionImplementation() {
    return originalTypeDescriptor.isJsFunctionImplementation();
  }

  @Override
  public MethodDescriptor getJsFunctionMethodDescriptor() {
    return originalTypeDescriptor.getJsFunctionMethodDescriptor();
  }

  @Override
  public MethodDescriptor getConcreteJsFunctionMethodDescriptor() {
    return originalTypeDescriptor.getConcreteJsFunctionMethodDescriptor();
  }

  @Override
  public boolean subclassesJsConstructorClass() {
    return originalTypeDescriptor.subclassesJsConstructorClass();
  }

  @Override
  public boolean isLocal() {
    return originalTypeDescriptor.isLocal();
  }
}
