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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.processors.Visitable;

import java.util.List;

/**
 * Abstract base class for type reference.
 *
 * Only abstract methods are allowed here. For simplicity of design (avoiding hierarchy) it is
 * *required* that all implementation be in subclasses.
 */
@Visitable
public abstract class TypeDescriptor extends Node implements Comparable<TypeDescriptor>, HasJsName {

  @Override
  public Node accept(Processor processor) {
    return Visitor_TypeDescriptor.visit(processor, this);
  }

  @Override
  public int compareTo(TypeDescriptor that) {
    return getUniqueId().compareTo(that.getUniqueId());
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TypeDescriptor) {
      return getUniqueId().equals(((TypeDescriptor) o).getUniqueId());
    }
    return false;
  }

  /**
   * Returns whether the two given type descriptors are the same, ignoring nullability.
   */
  public boolean equalsIgnoreNullability(TypeDescriptor other) {
    return this.getNonNullable().equals(other.getNonNullable());
  }

  /**
   * Returns the fully package qualified binary name like "com.google.common.Outer$Inner".
   */
  public abstract String getBinaryName();

  public abstract ImmutableList<String> getClassComponents();

  /**
   * Returns the unqualified binary name like "Outer$Inner".
   */
  public abstract String getClassName();

  public abstract MethodDescriptor getConcreteJsFunctionMethodDescriptor();

  public abstract Expression getDefaultValue();

  public abstract int getDimensions();

  public abstract TypeDescriptor getEnclosingTypeDescriptor();

  public abstract TypeDescriptor getForArray(int dimensions);

  public abstract MethodDescriptor getJsFunctionMethodDescriptor();

  @Override
  public abstract String getJsName();

  @Override
  public abstract String getJsNamespace();

  public abstract ImmutableList<String> getPackageComponents();

  /**
   * Returns the fully package qualified name like "com.google.common".
   */
  public abstract String getPackageName();

  public abstract String getQualifiedName();

  public abstract TypeDescriptor getRawTypeDescriptor();

  /**
   * Returns the unqualified and unenclosed simple name like "Inner".
   */
  public abstract String getSimpleName();

  /**
   * Returns the fully package qualified source name like "com.google.common.Outer.Inner".
   */
  public abstract String getSourceName();

  public abstract TypeDescriptor getSuperTypeDescriptor();

  public abstract List<TypeDescriptor> getTypeArgumentDescriptors();

  public abstract String getUniqueId();

  public abstract Visibility getVisibility();

  @Override
  public int hashCode() {
    return Objects.hashCode(getUniqueId());
  }

  /**
   * Returns whether this is an array type descriptor. Only true when it is an instance of the
   * ArrayTypeDescriptor subclass.
   */
  public abstract boolean isArray();

  public abstract boolean isEnumOrSubclass();

  /**
   * TODO: Currently we depends on the namespace to tell if a type is an extern type. Returns true
   * if the namespace is an empty string. It is true for most common cases, but not always true. We
   * may need to introduce a new annotation to tell if it is extern when we hit the problem.
   */
  public abstract boolean isExtern();

  public abstract boolean isGlobal();

  public abstract boolean isInstanceMemberClass();

  public abstract boolean isInstanceNestedClass();

  public abstract boolean isInterface();

  public abstract boolean isJsFunctionImplementation();

  public abstract boolean isJsFunctionInterface();

  public abstract boolean isJsType();

  public abstract boolean isLocal();

  public abstract boolean isNative();

  public abstract boolean isParameterizedType();

  public abstract boolean isPrimitive();

  /**
   * Returns whether this is a Raw reference. Raw references are not mangled in the output and
   * thus can be used to describe reference to JS apis.
   */
  public abstract boolean isRaw();

  public abstract boolean isRawType();

  public abstract boolean isTypeVariable();

  /**
   * Returns whether this is a union type descriptor. Only true when it is an instance of the
   * UnionTypeDescriptor subclass.
   */
  public abstract boolean isUnion();
  
  public abstract boolean isNullable();

  public abstract NonNullableTypeDescriptor getNonNullable();

  public abstract boolean isWildCard();

  public abstract boolean subclassesJsConstructorClass();
}
