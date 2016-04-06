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

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.processors.Visitable;

import java.util.Collections;
import java.util.List;

/**
 * A (by name) reference to an Array type.
 */
@AutoValue
@Visitable
public abstract class ArrayTypeDescriptor extends TypeDescriptor {

  @Override
  public Node accept(Processor processor) {
    return Visitor_ArrayTypeDescriptor.visit(processor, this);
  }

  @Override
  public String getBinaryName() {
    return getPrefix() + getLeafTypeDescriptor().getBinaryName();
  }

  @Override
  public ImmutableList<String> getClassComponents() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getClassName() {
    return getLeafTypeDescriptor().getClassName() + getSuffix();
  }

  public TypeDescriptor getComponentTypeDescriptor() {
    return getLeafTypeDescriptor().getForArray(getDimensions() - 1);
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
  public abstract int getDimensions();

  @Override
  public TypeDescriptor getEnclosingTypeDescriptor() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TypeDescriptor getForArray(int dimensions) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MethodDescriptor getJsFunctionMethodDescriptor() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getJsName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getJsNamespace() {
    throw new UnsupportedOperationException();
  }

  public abstract TypeDescriptor getLeafTypeDescriptor();

  @Override
  public ImmutableList<String> getPackageComponents() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getPackageName() {
    return getLeafTypeDescriptor().getPackageName();
  }

  private String getPrefix() {
    return Strings.repeat("[", getDimensions());
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
    return getLeafTypeDescriptor().getSimpleName() + getSuffix();
  }

  @Override
  public String getSourceName() {
    return getLeafTypeDescriptor().getSourceName() + getSuffix();
  }

  private String getSuffix() {
    return Strings.repeat("[]", getDimensions());
  }

  @Override
  public TypeDescriptor getSuperTypeDescriptor() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<TypeDescriptor> getTypeArgumentDescriptors() {
    return Collections.emptyList();
  }

  @Override
  public String getUniqueId() {
    return getLeafTypeDescriptor().getUniqueId() + getSuffix();
  }

  @Override
  public Visibility getVisibility() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isArray() {
    return true;
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
    return false;
  }

  @Override
  public boolean isJsType() {
    return false;
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public boolean isNative() {
    return false;
  }

  @Override
  public boolean isParameterizedType() {
    return false;
  }

  @Override
  public boolean isPrimitive() {
    return false;
  }

  @Override
  public boolean isRaw() {
    return getLeafTypeDescriptor().isRaw();
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
  public boolean isNullable() {
    return true;
  }

  @Override
  public NonNullableTypeDescriptor getNonNullable() {
    return NonNullableTypeDescriptor.create(this);
  }

  @Override
  public String toString() {
    return getLeafTypeDescriptor().toString() + Strings.repeat("[]", getDimensions());
  }
}
