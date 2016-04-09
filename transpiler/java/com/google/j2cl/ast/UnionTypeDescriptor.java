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
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.processors.Visitable;

import java.util.Collections;
import java.util.List;

/**
 * A (by name) reference to an union type, which is used in catch clause.
 */
@AutoValue
@Visitable
public abstract class UnionTypeDescriptor extends TypeDescriptor {
  public static UnionTypeDescriptor create(List<TypeDescriptor> typeDescriptors) {
    return new AutoValue_UnionTypeDescriptor(ImmutableList.copyOf(typeDescriptors));
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_UnionTypeDescriptor.visit(processor, this);
  }

  @Override
  public String getBinaryName() {
    return Joiner.on(" | ")
        .join(
            Lists.transform(
                getTypes(),
                new Function<TypeDescriptor, String>() {
                  @Override
                  public String apply(TypeDescriptor typeDescriptor) {
                    return typeDescriptor.getBinaryName();
                  }
                }));
  }

  @Override
  public ImmutableList<String> getClassComponents() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getClassName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public MethodDescriptor getConcreteJsFunctionMethodDescriptor() {
    throw new UnsupportedOperationException();
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

  @Override
  public ImmutableList<String> getPackageComponents() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getPackageName() {
    throw new UnsupportedOperationException();
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
    throw new UnsupportedOperationException();
  }

  @Override
  public String getSourceName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TypeDescriptor getSuperTypeDescriptor() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<TypeDescriptor> getTypeArgumentDescriptors() {
    return Collections.emptyList();
  }

  public abstract ImmutableList<TypeDescriptor> getTypes();

  @Override
  public String getUniqueId() {
    return Joiner.on(" | ")
        .join(
            Lists.transform(
                getTypes(),
                new Function<TypeDescriptor, String>() {
                  @Override
                  public String apply(TypeDescriptor typeDescriptor) {
                    return typeDescriptor.getUniqueId();
                  }
                }));
  }

  @Override
  public Visibility getVisibility() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isArray() {
    return false;
  }

  @Override
  public boolean isEnumOrSubclass() {
    return false;
  }

  /**
   * TODO: Currently we depends on the namespace to tell if a type is an extern type. Returns true
   * if the namespace is an empty string. It is true for most common cases, but not always true. We
   * may need to introduce a new annotation to tell if it is extern when we hit the problem.
   */
  @Override
  public boolean isExtern() {
    boolean isSynthesizedGlobalType = isRaw() && JsInteropUtils.isGlobal(getPackageName());
    boolean isNativeJsType = isNative() && JsInteropUtils.isGlobal(getJsNamespace());
    return isSynthesizedGlobalType || isNativeJsType;
  }

  @Override
  public boolean isGlobal() {
    return "".equals(getQualifiedName());
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
    return true;
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
    return "("
        + Joiner.on("|")
            .join(
                FluentIterable.from(getTypes())
                    .transform(
                        new Function<TypeDescriptor, String>() {
                          @Override
                          public String apply(TypeDescriptor typeDescriptor) {
                            return typeDescriptor.toString();
                          }
                        }));
  }
}
