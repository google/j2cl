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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.processors.Visitable;

import java.util.Collections;

/**
 * Represents a synthetic parametric TypeDescriptor.
 *
 * <p>A SyntheticParametricTypeDescriptor wraps the original TypeDescriptor, with a given
 * type arguments list. It returns consistent result as its original type.
 */
@Visitable
public class SyntheticParametricTypeDescriptor extends TypeDescriptor {
  private final ImmutableList<String> classComponents;

  // JsInterop properties
  private final boolean isJsFunction;
  private final boolean isJsType;
  private final boolean isNative;
  private final String jsTypeName;
  private final String jsTypeNamespace;

  private final TypeDescriptor originalTypeDescriptor;
  private final ImmutableList<String> packageComponents;
  private final ImmutableList<TypeDescriptor> typeArgumentDescriptors;

  SyntheticParametricTypeDescriptor(
      TypeDescriptor originalTypeDescriptor, Iterable<TypeDescriptor> typeArgumentTypeDescriptors) {
    this.originalTypeDescriptor = originalTypeDescriptor;
    this.packageComponents = originalTypeDescriptor.getPackageComponents();
    this.classComponents = originalTypeDescriptor.getClassComponents();
    this.isJsFunction = originalTypeDescriptor.isJsFunctionInterface();
    this.isJsType = originalTypeDescriptor.isJsType();
    this.isNative = originalTypeDescriptor.isNative();
    this.jsTypeNamespace = originalTypeDescriptor.getJsNamespace();
    this.jsTypeName = originalTypeDescriptor.getJsName();
    this.typeArgumentDescriptors = ImmutableList.copyOf(typeArgumentTypeDescriptors);
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_SyntheticParametricTypeDescriptor.visit(processor, this);
  }

  @Override
  public String getBinaryName() {
    return TypeDescriptors.getBinaryName(this);
  }

  @Override
  public ImmutableList<String> getClassComponents() {
    return classComponents;
  }

  @Override
  public String getClassName() {
    return TypeDescriptors.getClassName(this);
  }

  @Override
  public MethodDescriptor getConcreteJsFunctionMethodDescriptor() {
    return originalTypeDescriptor.getConcreteJsFunctionMethodDescriptor();
  }

  @Override
  public Expression getDefaultValue() {
    return NullLiteral.NULL;
  }

  @Override
  public int getDimensions() {
    return 0;
  }

  @Override
  public TypeDescriptor getEnclosingTypeDescriptor() {
    return originalTypeDescriptor.getEnclosingTypeDescriptor();
  }

  @Override
  public TypeDescriptor getForArray(int dimensions) {
    return TypeDescriptors.getForArray(this, dimensions);
  }

  @Override
  public MethodDescriptor getJsFunctionMethodDescriptor() {
    return originalTypeDescriptor.getJsFunctionMethodDescriptor();
  }

  @Override
  public String getJsName() {
    return jsTypeName;
  }

  @Override
  public String getJsNamespace() {
    return jsTypeNamespace;
  }

  @Override
  public ImmutableList<String> getPackageComponents() {
    return packageComponents;
  }

  @Override
  public String getPackageName() {
    return Joiner.on(".").join(getPackageComponents());
  }

  @Override
  public String getQualifiedName() {
    return TypeDescriptors.getQualifiedName(this);
  }

  @Override
  public TypeDescriptor getRawTypeDescriptor() {
    return TypeDescriptors.createSyntheticParametricTypeDescriptor(
        originalTypeDescriptor, Collections.emptyList());
  }

  @Override
  public String getSimpleName() {
    return Iterables.getLast(getClassComponents());
  }

  @Override
  public String getSourceName() {
    return Joiner.on(".").join(Iterables.concat(getPackageComponents(), getClassComponents()));
  }

  @Override
  public TypeDescriptor getSuperTypeDescriptor() {
    return originalTypeDescriptor.getSuperTypeDescriptor();
  }

  @Override
  public ImmutableList<TypeDescriptor> getTypeArgumentDescriptors() {
    return typeArgumentDescriptors;
  }

  @Override
  public String getUniqueId() {
    // For type variable, we use its JDT binary name plus its erasure's binary name, which will
    // ensure the uniqueness of the type variable. Since in j2cl we only care about the left bound
    // of the type variable (which is returned by getErasure()), it should be enough for uniqueness.
    if (isTypeVariable()) {
      throw new UnsupportedOperationException();
    }
    return getBinaryName() + TypeDescriptors.getTypeArgumentsUniqueId(this);
  }

  @Override
  public Visibility getVisibility() {
    return originalTypeDescriptor.getVisibility();
  }

  @Override
  public boolean isArray() {
    return false;
  }

  @Override
  public boolean isEnumOrSubclass() {
    return originalTypeDescriptor.isEnumOrSubclass();
  }

  @Override
  public boolean isExtern() {
    return TypeDescriptors.isExtern(this);
  }

  @Override
  public boolean isGlobal() {
    return TypeDescriptors.isGlobal(this);
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
  public boolean isJsFunctionImplementation() {
    return originalTypeDescriptor.isJsFunctionImplementation();
  }

  @Override
  public boolean isJsFunctionInterface() {
    return isJsFunction;
  }

  @Override
  public boolean isJsType() {
    return isJsType;
  }

  @Override
  public boolean isLocal() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isNative() {
    return isNative;
  }

  @Override
  public boolean isParameterizedType() {
    return !getTypeArgumentDescriptors().isEmpty();
  }

  @Override
  public boolean isPrimitive() {
    return false;
  }

  @Override
  public boolean isRaw() {
    return false;
  }

  @Override
  public boolean isRawType() {
    return false;
  }

  @Override
  public boolean isTypeVariable() {
    return false;
  }

  @Override
  public boolean isUnion() {
    return false;
  }

  @Override
  public boolean isWildCard() {
    return false;
  }

  @Override
  public boolean subclassesJsConstructorClass() {
    return originalTypeDescriptor.subclassesJsConstructorClass();
  }

  @Override
  public String toString() {
    return getSourceName();
  }

  @Override
  public boolean isNullable() {
    return true;
  }

  @Override
  public NonNullableTypeDescriptor getNonNullable() {
    return NonNullableTypeDescriptor.create(this);
  }
}
