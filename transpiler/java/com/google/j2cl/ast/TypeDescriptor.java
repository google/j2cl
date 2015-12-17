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
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.processors.Visitable;

import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Base class for type reference.
 */
@Visitable
public abstract class TypeDescriptor extends Expression implements Comparable<TypeDescriptor> {
  public static final String VOID_TYPE_NAME = "void";
  public static final String INT_TYPE_NAME = "int";
  public static final String BOOLEAN_TYPE_NAME = "boolean";
  public static final String BYTE_TYPE_NAME = "byte";
  public static final String CHAR_TYPE_NAME = "char";
  public static final String DOUBLE_TYPE_NAME = "double";
  public static final String FLOAT_TYPE_NAME = "float";
  public static final String LONG_TYPE_NAME = "long";
  public static final String SHORT_TYPE_NAME = "short";

  private static Interner<TypeDescriptor> interner;

  // This is only used by TypeProxyUtils, and cannot be used elsewhere. Because to create a
  // TypeDescriptor from a TypeBinding, it should go through the path to check array type.
  static TypeDescriptor create(ITypeBinding typeBinding) {
    Preconditions.checkArgument(!typeBinding.isArray());
    return getInterner().intern(new RegularTypeDescriptor(typeBinding));
  }

  public static TypeDescriptor create(
      Iterable<String> packageComponents,
      Iterable<String> classComponents,
      boolean isRaw,
      boolean isNative,
      Iterable<TypeDescriptor> typeArgumentDescriptors) {
    Preconditions.checkArgument(!Iterables.getLast(classComponents).contains("<"));
    return getInterner()
        .intern(
            new SyntheticRegularTypeDescriptor(
                ImmutableList.copyOf(packageComponents),
                ImmutableList.copyOf(classComponents),
                isRaw,
                isNative,
                ImmutableList.copyOf(typeArgumentDescriptors)));
  }

  public static TypeDescriptor createSynthetic(
      Iterable<String> packageComponents, Iterable<String> classComponents) {
    return create(
        packageComponents, classComponents, false, false, ImmutableList.<TypeDescriptor>of());
  }

  public static TypeDescriptor createRaw(
      Iterable<String> nameSpaceComponents, String className, boolean isNative) {
    return create(
        nameSpaceComponents,
        Arrays.asList(className),
        true,
        isNative,
        ImmutableList.<TypeDescriptor>of());
  }

  /**
   * Creates a native TypeDescriptor from a qualified name.
   */
  public static TypeDescriptor createNative(String qualifiedName) {
    boolean isNative = true;
    if (JsInteropUtils.isGlobal(qualifiedName)) {
      return TypeDescriptor.createRaw(Arrays.asList(JsInteropUtils.JS_GLOBAL), "", isNative);
    }
    List<String> nameComponents = Splitter.on('.').splitToList(qualifiedName);
    int size = nameComponents.size();
    // Fill in JS_GLOBAL as the namespace if the namespace is empty.
    List<String> namespaceComponents =
        size == 1 ? Arrays.asList(JsInteropUtils.JS_GLOBAL) : nameComponents.subList(0, size - 1);
    return TypeDescriptor.createRaw(namespaceComponents, nameComponents.get(size - 1), isNative);
  }

  static Interner<TypeDescriptor> getInterner() {
    if (interner == null) {
      interner = Interners.newWeakInterner();
    }
    return interner;
  }

  public abstract String getBinaryName();

  public abstract String getClassName();

  public abstract String getSimpleName();

  public abstract String getSourceName();

  public abstract String getPackageName();

  public abstract boolean isArray();

  /**
   * Returns whether this is a Raw reference. Raw references are not mangled in the output and
   * thus can be used to describe reference to JS apis.
   */
  public abstract boolean isRaw();

  public boolean isPrimitive() {
    return false;
  }

  public boolean isParameterizedType() {
    return false;
  }

  public boolean isTypeVariable() {
    return false;
  }

  public boolean isWildCard() {
    return false;
  }

  public abstract int getDimensions();

  public abstract TypeDescriptor getLeafTypeDescriptor();

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return this;
  }

  public TypeDescriptor getComponentTypeDescriptor() {
    return getLeafTypeDescriptor();
  }

  public Expression getDefaultValue() {
    // Primitives.
    switch (this.getSourceName()) {
      case TypeDescriptor.BOOLEAN_TYPE_NAME:
        return BooleanLiteral.FALSE;
      case TypeDescriptor.BYTE_TYPE_NAME:
      case TypeDescriptor.SHORT_TYPE_NAME:
      case TypeDescriptor.INT_TYPE_NAME:
      case TypeDescriptor.FLOAT_TYPE_NAME:
      case TypeDescriptor.DOUBLE_TYPE_NAME:
      case TypeDescriptor.CHAR_TYPE_NAME:
        return new NumberLiteral(this, 0);
      case TypeDescriptor.LONG_TYPE_NAME:
        return new NumberLiteral(this, 0L);
    }

    // Objects.
    return NullLiteral.NULL;
  }

  public TypeDescriptor getRawTypeDescriptor() {
    return this;
  }

  public TypeDescriptor getSuperTypeDescriptor() {
    return null;
  }

  public TypeDescriptor getEnclosingTypeDescriptor() {
    return null;
  }

  public Visibility getVisibility() {
    return null;
  }

  public boolean isJsType() {
    return false;
  }

  public boolean isJsFunctionInterface() {
    return false;
  }

  public boolean isJsFunctionImplementation() {
    return false;
  }

  public MethodDescriptor getJsFunctionMethodDescriptor() {
    return null;
  }

  public boolean isNative() {
    return false;
  }

  public String getJsTypeNamespace() {
    return null;
  }

  public String getJsTypeName() {
    return null;
  }

  /**
   * TODO: Currently we depends on the namespace to tell if a type is an extern type. Returns true
   * if the namespace is an empty string. It is true for most common cases, but not always true. We
   * may need to introduce a new annotation to tell if it is extern when we hit the problem.
   */
  public boolean isExtern() {
    boolean isSynthesizedGlobalType = isRaw() && JsInteropUtils.isGlobal(getPackageName());
    boolean isNativeJsType = isNative() && JsInteropUtils.isGlobal(getJsTypeNamespace());
    return isSynthesizedGlobalType || isNativeJsType;
  }

  /**
   * Returns true if it is a native JsType interface with namespace GLOBAL.
   *
   *<p>This is used for the workaround that not generating goog.require for "literal" native types
   * so that users do not need to provide goog.module for it.
   */
  public boolean isGloballNativeInterface() {
    return isNative() && isInterface() && JsInteropUtils.isGlobal(getJsTypeNamespace());
  }

  public String getQualifiedName() {
    String namespace = getJsTypeNamespace() != null ? getJsTypeNamespace() : getPackageName();
    namespace = JsInteropUtils.isGlobal(namespace) ? "" : namespace;
    String className = getJsTypeName() != null ? getJsTypeName() : getClassName();
    return Joiner.on(".")
        .skipNulls()
        .join(Strings.emptyToNull(namespace), Strings.emptyToNull(className));
  }

  public boolean isGlobal() {
    return "".equals(getQualifiedName());
  }

  public boolean isInstanceMemberClass() {
    return false;
  }

  public boolean isInstanceNestedClass() {
    return false;
  }

  public boolean isInterface() {
    return false;
  }

  public List<TypeDescriptor> getTypeArgumentDescriptors() {
    return Collections.emptyList();
  }

  public TypeDescriptor getForArray(int dimensions) {
    if (dimensions == 0) {
      return this;
    }
    return getInterner().intern(new AutoValue_ArrayTypeDescriptor(dimensions, this));
  }

  @Override
  public int compareTo(TypeDescriptor that) {
    return getSourceName().compareTo(that.getSourceName());
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TypeDescriptor) {
      return getSourceName().equals(((TypeDescriptor) o).getSourceName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getSourceName());
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_TypeDescriptor.visit(processor, this);
  }
}
