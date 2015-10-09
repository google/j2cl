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
 * Synthesized RegularTypeDescriptor.
 * <p>
 * This class represents the type descriptor that does not have a JDT typeBinding mapping,
 * for example, the nativebootstrap and vmbootstrap type descriptors.
 */
public class SyntheticRegularTypeDescriptor extends RegularTypeDescriptor {

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
  public TypeDescriptor getSuperTypeDescriptor() {
    throw new UnsupportedOperationException(
        "Synthetic type descriptors do not know their super type.");
  }

  @Override
  public TypeDescriptor getEnclosingTypeDescriptor() {
    throw new UnsupportedOperationException(
        "Synthetic type descriptors do not know their enclosing type.");
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
  public boolean isRaw() {
    return isRaw;
  }
}
