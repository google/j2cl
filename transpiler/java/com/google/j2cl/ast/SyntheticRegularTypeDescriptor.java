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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.processors.Visitable;

import java.util.Collections;

/**
 * Represents the type descriptor that does not have a JDT typeBinding mapping. It is used to
 * synthesize the nativebootstrap, vmbootstrap type descriptors, synthetic native js types and
 * overlay types.
 * TODO: reexamine if we need a separate class for overlay type.
 */
@Visitable
public class SyntheticRegularTypeDescriptor extends TypeDescriptor {
  private final ImmutableList<String> classComponents;

  // JsInterop properties
  private boolean isJsFunction;
  private boolean isJsType;
  private boolean isNative;
  private final boolean isRaw;
  private String jsTypeName;
  private String jsTypeNamespace;

  private final ImmutableList<String> packageComponents;
  private final ImmutableList<TypeDescriptor> typeArgumentDescriptors;

  /**
   * Constructor for a regular synthetic type that does not contain any JsInterop information.
   */
  SyntheticRegularTypeDescriptor(
      Iterable<String> packageComponents,
      Iterable<String> classComponents,
      boolean isRaw,
      Iterable<TypeDescriptor> typeArgumentDescriptors) {
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
  public Node accept(Processor processor) {
    return Visitor_SyntheticRegularTypeDescriptor.visit(processor, this);
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
    throw new UnsupportedOperationException();
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
    throw new UnsupportedOperationException();
  }

  @Override
  public TypeDescriptor getForArray(int dimensions) {
    return TypeDescriptors.getForArray(this, dimensions);
  }

  @Override
  public MethodDescriptor getJsFunctionMethodDescriptor() {
    throw new UnsupportedOperationException();
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
    return isJsType
        ? TypeDescriptors.createSyntheticNativeTypeDescriptor(
            getPackageComponents(),
            getClassComponents(),
            Collections.emptyList(),
            jsTypeNamespace,
            jsTypeName)
        : TypeDescriptors.createSyntheticRegularTypeDescriptor(
            getPackageComponents(), getClassComponents(), isRaw(), Collections.emptyList());
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
    throw new UnsupportedOperationException();
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
    return Visibility.PUBLIC;
  }

  @Override
  public boolean isArray() {
    return false;
  }

  @Override
  public boolean isEnumOrSubclass() {
    return false;
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
    return false;
  }

  @Override
  public boolean isInstanceNestedClass() {
    return false;
  }

  @Override
  public boolean isInterface() {
    return false;
  }

  @Override
  public boolean isJsFunctionImplementation() {
    return false;
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
    return false;
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
    return isRaw;
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
    return false;
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
