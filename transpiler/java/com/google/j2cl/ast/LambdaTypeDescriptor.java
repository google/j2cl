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

import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.Arrays;

/**
 * Represents a TypeDescriptor for Lambda Type.
 */
@Visitable
public class LambdaTypeDescriptor extends TypeDescriptor {
  private ImmutableList<String> classComponents;

  // JsInterop properties
  private boolean isNative;

  private ITypeBinding lambdaInterfaceBinding;
  private ImmutableList<String> packageComponents;
  private ImmutableList<TypeDescriptor> typeArgumentDescriptors;

  LambdaTypeDescriptor(
      TypeDescriptor enclosingClassTypeDescriptor,
      String lambdaBinaryName,
      ITypeBinding lambdaInterfaceBinding) {
    this.lambdaInterfaceBinding = lambdaInterfaceBinding;
    this.packageComponents =
        ImmutableList.copyOf(enclosingClassTypeDescriptor.getPackageComponents());
    this.classComponents =
        ImmutableList.copyOf(
            Iterables.concat(
                enclosingClassTypeDescriptor.getClassComponents(),
                Arrays.asList(lambdaBinaryName)));
    this.isNative = false;
    this.typeArgumentDescriptors = ImmutableList.of();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_LambdaTypeDescriptor.visit(processor, this);
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
  public TypeDescriptor getComponentTypeDescriptor() {
    return null;
  }

  @Override
  public MethodDescriptor getConcreteJsFunctionMethodDescriptor() {
    return TypeProxyUtils.getConcreteJsFunctionMethodDescriptor(lambdaInterfaceBinding);
  }

  @Override
  public int getDimensions() {
    return 0;
  }

  @Override
  public TypeDescriptor getEnclosingTypeDescriptor() {
    return null;
  }

  @Override
  public MethodDescriptor getJsFunctionMethodDescriptor() {
    return TypeProxyUtils.getJsFunctionMethodDescriptor(lambdaInterfaceBinding);
  }

  @Override
  public String getJsName() {
    return null;
  }

  @Override
  public String getJsNamespace() {
    return null;
  }

  @Override
  public ImmutableList<String> getPackageComponents() {
    return packageComponents;
  }

  @Override
  public TypeDescriptor getLeafTypeDescriptor() {
    return null;
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
    return this;
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
    return TypeDescriptors.get().javaLangObject;
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
    return Visibility.PRIVATE;
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
    return true;
  }

  @Override
  public boolean isInterface() {
    return false;
  }

  @Override
  public boolean isJsFunctionImplementation() {
    return JsInteropUtils.isJsFunction(lambdaInterfaceBinding);
  }

  @Override
  public boolean isJsFunctionInterface() {
    return false;
  }

  @Override
  public boolean isJsType() {
    return false;
  }

  @Override
  public boolean isLocal() {
    return true;
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
}
