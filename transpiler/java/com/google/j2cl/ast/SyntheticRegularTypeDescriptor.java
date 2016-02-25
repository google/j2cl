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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Represents the type descriptor that does not have a JDT typeBinding mapping. It is used to
 * synthesize the nativebootstrap, vmbootstrap type descriptors, synthetic native js types and
 * overlay types.
 * TODO: reexamine if we need a separate class for overlay type.
 */
public class SyntheticRegularTypeDescriptor extends RegularTypeDescriptor {
  /**
   * Constructor for a regular synthetic type that does not contain any JsInterop information.
   */
  SyntheticRegularTypeDescriptor(
      Iterable<String> packageComponents,
      Iterable<String> classComponents,
      boolean isRaw,
      Iterable<TypeDescriptor> typeArgumentDescriptors) {
    super(null);
    this.packageComponents = ImmutableList.copyOf(packageComponents);
    this.classComponents = ImmutableList.copyOf(classComponents);
    this.isRaw = isRaw;
    this.typeArgumentDescriptors = ImmutableList.copyOf(typeArgumentDescriptors);
  }

  /**
   * Constructor for a synthetic native js type.
   */
  SyntheticRegularTypeDescriptor(
      Iterable<String> packageComponents,
      Iterable<String> classComponents,
      Iterable<TypeDescriptor> typeArgumentDescriptors,
      String jsTypeNamespace,
      String jsTypeName) {
    this(packageComponents, classComponents, false, typeArgumentDescriptors);
    this.isNative = true;
    this.isJsType = true;
    this.jsTypeNamespace = jsTypeNamespace;
    this.jsTypeName = jsTypeName;
  }

  @Override
  public ImmutableList<String> getPackageComponents() {
    return packageComponents;
  }

  @Override
  public ImmutableList<String> getClassComponents() {
    return classComponents;
  }

  @Override
  public ImmutableList<TypeDescriptor> getTypeArgumentDescriptors() {
    return typeArgumentDescriptors;
  }

  @Override
  public TypeDescriptor getRawTypeDescriptor() {
    return isJsType
        ? TypeDescriptor.createSyntheticNativeTypeDescriptor(
            getPackageComponents(),
            getClassComponents(),
            ImmutableList.<TypeDescriptor>of(),
            jsTypeNamespace,
            jsTypeName)
        : TypeDescriptor.createSyntheticRegularTypeDescriptor(
            getPackageComponents(),
            getClassComponents(),
            isRaw(),
            ImmutableList.<TypeDescriptor>of());
  }

  @Override
  public TypeDescriptor getSuperTypeDescriptor() {
    return null;
  }

  @Override
  public TypeDescriptor getEnclosingTypeDescriptor() {
    return null;
  }

  @Override
  public Visibility getVisibility() {
    return Visibility.PUBLIC;
  }

  @Override
  public boolean isInstanceMemberClass() {
    return false;
  }

  @Override
  public boolean isInstanceNestedClass() {
    return false;
  }

  @Override
  public boolean isEnumOrSubclass() {
    return false;
  }

  @Override
  public boolean isJsFunctionImplementation() {
    return false;
  }

  @Override
  public MethodDescriptor getJsFunctionMethodDescriptor() {
    Preconditions.checkArgument(!isJsFunctionInterface() && !isJsFunctionImplementation());
    return null;
  }

  @Override
  public MethodDescriptor getConcreteJsFunctionMethodDescriptor() {
    Preconditions.checkArgument(!isJsFunctionInterface() && !isJsFunctionImplementation());
    return null;
  }

  @Override
  public boolean subclassesJsConstructorClass() {
    return false;
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public boolean isRaw() {
    return isRaw;
  }
}
