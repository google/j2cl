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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.processors.Visitable;
import com.google.j2cl.common.JsInteropAnnotationUtils;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * A (by name) reference to a class.
 */
@Visitable
public class RegularTypeDescriptor extends TypeDescriptor {
  private ImmutableList<String> classComponents;

  // JsInterop properties
  private boolean isJsFunction;
  private boolean isJsType;
  private boolean isNative;
  private String jsTypeName;
  private String jsTypeNamespace;

  private ImmutableList<String> packageComponents;
  private ImmutableList<TypeDescriptor> typeArgumentDescriptors;
  private ITypeBinding typeBinding;

  RegularTypeDescriptor(ITypeBinding typeBinding) {
    this.typeBinding = typeBinding;
    if (typeBinding != null) {
      setJsInteropProperties();
    }
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_RegularTypeDescriptor.visit(processor, this);
  }

  @Override
  public String getBinaryName() {
    return TypeDescriptors.getBinaryName(this);
  }

  @Override
  public ImmutableList<String> getClassComponents() {
    // Lazily initialize classComponents.
    if (classComponents == null) {
      Preconditions.checkNotNull(typeBinding);
      classComponents = ImmutableList.copyOf(TypeProxyUtils.getClassComponents(typeBinding));
    }
    return classComponents;
  }

  @Override
  public String getClassName() {
    return TypeDescriptors.getClassName(this);
  }

  @Override
  public MethodDescriptor getConcreteJsFunctionMethodDescriptor() {
    return TypeProxyUtils.getConcreteJsFunctionMethodDescriptor(typeBinding);
  }

  @Override
  public Expression getDefaultValue() {
    // Primitives.
    switch (this.getSourceName()) {
      case TypeDescriptors.BOOLEAN_TYPE_NAME:
        return BooleanLiteral.FALSE;
      case TypeDescriptors.BYTE_TYPE_NAME:
      case TypeDescriptors.SHORT_TYPE_NAME:
      case TypeDescriptors.INT_TYPE_NAME:
      case TypeDescriptors.FLOAT_TYPE_NAME:
      case TypeDescriptors.DOUBLE_TYPE_NAME:
      case TypeDescriptors.CHAR_TYPE_NAME:
        return new NumberLiteral(this, 0);
      case TypeDescriptors.LONG_TYPE_NAME:
        return new NumberLiteral(this, 0L);
    }

    // Objects.
    return NullLiteral.NULL;
  }

  @Override
  public int getDimensions() {
    return 0;
  }

  @Override
  public TypeDescriptor getEnclosingTypeDescriptor() {
    // Will return a consistent interned copy, should be decently fast.
    return TypeProxyUtils.createTypeDescriptor(typeBinding.getDeclaringClass());
  }

  @Override
  public TypeDescriptor getForArray(int dimensions) {
    return TypeDescriptors.getForArray(this, dimensions);
  }

  @Override
  public MethodDescriptor getJsFunctionMethodDescriptor() {
    return TypeProxyUtils.getJsFunctionMethodDescriptor(typeBinding);
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
    // Lazily initialize packageComponents.
    if (packageComponents == null) {
      Preconditions.checkNotNull(typeBinding);
      packageComponents = ImmutableList.copyOf(TypeProxyUtils.getPackageComponents(typeBinding));
    }
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

  /**
   * Returns the erasure type (see definition of erasure type at
   * http://help.eclipse.org/luna/index.jsp) with an empty type arguments list.
   */
  @Override
  public TypeDescriptor getRawTypeDescriptor() {
    TypeDescriptor rawTypeDescriptor =
        TypeProxyUtils.createTypeDescriptor(typeBinding.getErasure());
    if (rawTypeDescriptor.isParameterizedType()) {
      return TypeDescriptors.createSyntheticParametricTypeDescriptor(
          rawTypeDescriptor, ImmutableList.<TypeDescriptor>of());
    }
    return rawTypeDescriptor;
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
    // Will return a consistent interned copy, should be decently fast.
    return TypeProxyUtils.createTypeDescriptor(typeBinding.getSuperclass());
  }

  @Override
  public ImmutableList<TypeDescriptor> getTypeArgumentDescriptors() {
    // Lazily initialize typeArgumentDescriptors.
    if (typeArgumentDescriptors == null) {
      Preconditions.checkNotNull(typeBinding);
      typeArgumentDescriptors =
          ImmutableList.copyOf(TypeProxyUtils.getTypeArgumentDescriptors(typeBinding));
    }
    return typeArgumentDescriptors;
  }

  @Override
  public String getUniqueId() {
    // For type variable, we use its JDT binary name plus its erasure's binary name, which will
    // ensure the uniqueness of the type variable. Since in j2cl we only care about the left bound
    // of the type variable (which is returned by getErasure()), it should be enough for uniqueness.
    if (isTypeVariable()) {
      // Binary name of a type variable is in the form of
      // EnclosingTypeBinaryName$DeclaringMethodDescriptor$SimpleName.
      // For example 'public static final <T> Foo<T> hashFunction()' in class 'test.Bar'
      // binary name of T is "test.Bar$()LFoo;$T"
      return typeBinding.getBinaryName() + ":" + typeBinding.getErasure().getBinaryName();
    }
    return getBinaryName() + TypeDescriptors.getTypeArgumentsUniqueId(this);
  }

  @Override
  public Visibility getVisibility() {
    return TypeProxyUtils.getVisibility(typeBinding.getModifiers());
  }

  @Override
  public boolean isArray() {
    return false;
  }

  @Override
  public boolean isEnumOrSubclass() {
    return TypeProxyUtils.isEnumOrSubclass(typeBinding);
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
    return TypeProxyUtils.isInstanceMemberClass(typeBinding);
  }

  @Override
  public boolean isInstanceNestedClass() {
    return TypeProxyUtils.isInstanceNestedClass(typeBinding);
  }

  @Override
  public boolean isInterface() {
    return typeBinding != null && typeBinding.isInterface();
  }

  @Override
  public boolean isJsFunctionImplementation() {
    return TypeProxyUtils.isJsFunctionImplementation(typeBinding);
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
    return TypeProxyUtils.isLocal(typeBinding);
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
    return typeBinding != null && typeBinding.isPrimitive();
  }

  @Override
  public boolean isRaw() {
    return false;
  }

  @Override
  public boolean isRawType() {
    return (typeBinding != null && typeBinding.isRawType());
  }

  @Override
  public boolean isTypeVariable() {
    return typeBinding != null && typeBinding.isTypeVariable();
  }

  @Override
  public boolean isUnion() {
    return false;
  }

  @Override
  public boolean isWildCard() {
    return (typeBinding != null && (typeBinding.isWildcardType() || typeBinding.isCapture()));
  }

  private void setJsInteropProperties() {
    IAnnotationBinding jsTypeAnnotation = JsInteropAnnotationUtils.getJsTypeAnnotation(typeBinding);
    if (jsTypeAnnotation != null) {
      isJsType = true;
      isNative = JsInteropAnnotationUtils.isNative(jsTypeAnnotation);
      jsTypeNamespace = JsInteropAnnotationUtils.getJsNamespace(jsTypeAnnotation);
      jsTypeName = JsInteropAnnotationUtils.getJsName(jsTypeAnnotation);
    }
    isJsFunction = JsInteropUtils.isJsFunction(typeBinding);
  }

  @Override
  public boolean subclassesJsConstructorClass() {
    return TypeProxyUtils.subclassesJsConstructorClass(typeBinding);
  }

  @Override
  public String toString() {
    return getSourceName();
  }
}
