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
import com.google.common.collect.Iterables;

import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.Arrays;

/**
 * Represents a TypeDescriptor for Lambda Type.
 */
public class LambdaTypeDescriptor extends RegularTypeDescriptor {
  private ITypeBinding lambdaInterfaceBinding;
  private RegularTypeDescriptor enclosingClassTypeDescriptor;

  LambdaTypeDescriptor(
      RegularTypeDescriptor enclosingClassTypeDescriptor,
      String lambdaBinaryName,
      ITypeBinding lambdaInterfaceBinding) {
    super(null);
    this.lambdaInterfaceBinding = lambdaInterfaceBinding;
    this.packageComponents =
        ImmutableList.copyOf(enclosingClassTypeDescriptor.getPackageComponents());
    this.classComponents =
        ImmutableList.copyOf(
            Iterables.concat(
                enclosingClassTypeDescriptor.getClassComponents(),
                Arrays.asList(lambdaBinaryName)));
    this.isRaw = false;
    this.isNative = false;
    this.typeArgumentDescriptors = ImmutableList.<TypeDescriptor>of();
  }

  @Override
  public TypeDescriptor getRawTypeDescriptor() {
    return this;
  }

  @Override
  public TypeDescriptor getSuperTypeDescriptor() {
    return TypeDescriptors.get().javaLangObject;
  }

  @Override
  public TypeDescriptor getEnclosingTypeDescriptor() {
    return this.enclosingClassTypeDescriptor;
  }

  @Override
  public Visibility getVisibility() {
    return Visibility.PRIVATE;
  }

  /**
   * A lambda class is not a member class.
   */
  @Override
  public boolean isInstanceMemberClass() {
    return false;
  }

  /**
   * A lambda class is an instance nested class.
   */
  @Override
  public boolean isInstanceNestedClass() {
    return true;
  }

  /**
   * A lambda class is not an interface.
   */
  @Override
  public boolean isInterface() {
    return false;
  }

  /**
   * A lambda class is not an enum or its subclass.
   */
  @Override
  public boolean isEnumOrSubclass() {
    return false;
  }

  @Override
  public boolean isJsFunctionImplementation() {
    return JsInteropUtils.isJsFunction(lambdaInterfaceBinding);
  }

  @Override
  public MethodDescriptor getJsFunctionMethodDescriptor() {
    return TypeProxyUtils.getJsFunctionMethodDescriptor(lambdaInterfaceBinding);
  }

  @Override
  public MethodDescriptor getConcreteJsFunctionMethodDescriptor() {
    return TypeProxyUtils.getConcreteJsFunctionMethodDescriptor(lambdaInterfaceBinding);
  }

  @Override
  public boolean subclassesJsConstructorClass() {
    return false;
  }

  /**
   * A lambda class is a local class.
   */
  @Override
  public boolean isLocal() {
    return true;
  }
}
