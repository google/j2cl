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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.processors.Visitable;

import java.util.ArrayList;
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
  public static final String JAVA_LANG_BOOLEAN_TYPE_NAME = "java.lang.Boolean";
  public static final String JAVA_LANG_DOUBLE_TYPE_NAME = "java.lang.Double";
  public static final String JAVA_LANG_NUMBER_TYPE_NAME = "java.lang.Number";

  private static Interner<TypeDescriptor> interner;

  private static TypeDescriptor create(
      Iterable<String> packageComponents,
      Iterable<String> classComponents,
      boolean isRaw,
      Iterable<TypeDescriptor> typeArgumentDescriptors,
      boolean isTypeVariable) {
    Preconditions.checkArgument(!Iterables.getLast(classComponents).contains("<"));
    return getInterner()
        .intern(
            new AutoValue_RegularTypeDescriptor(
                ImmutableList.copyOf(packageComponents),
                ImmutableList.copyOf(classComponents),
                isRaw,
                ImmutableList.copyOf(typeArgumentDescriptors),
                isTypeVariable));
  }

  public static TypeDescriptor create(
      Iterable<String> packageComponents, Iterable<String> classComponents) {
    return create(
        packageComponents, classComponents, false, ImmutableList.<TypeDescriptor>of(), false);
  }

  public static TypeDescriptor createRaw(Iterable<String> nameSpaceComponents, String className) {
    return create(
        nameSpaceComponents,
        Collections.singleton(className),
        true,
        ImmutableList.<TypeDescriptor>of(),
        false);
  }

  public static TypeDescriptor createParameterizedType(
      Iterable<String> packageComponents,
      Iterable<String> classComponents,
      Iterable<TypeDescriptor> typeArgumentDescriptors) {
    return create(
        packageComponents,
        classComponents,
        false,
        ImmutableList.copyOf(typeArgumentDescriptors),
        false);
  }

  public static TypeDescriptor createTypeVariable(
      Iterable<String> packageComponents, Iterable<String> classComponents) {
    return create(
        packageComponents, classComponents, false, ImmutableList.<TypeDescriptor>of(), true);
  }

  static TypeDescriptor createPrimitive(String primitiveTypeName) {
    return create(new ArrayList<String>(), Arrays.asList(primitiveTypeName));
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
    switch (getSourceName()) {
      case BOOLEAN_TYPE_NAME:
      case BYTE_TYPE_NAME:
      case CHAR_TYPE_NAME:
      case DOUBLE_TYPE_NAME:
      case FLOAT_TYPE_NAME:
      case INT_TYPE_NAME:
      case LONG_TYPE_NAME:
      case SHORT_TYPE_NAME:
      case VOID_TYPE_NAME:
        return true;
      default:
        return false;
    }
  }

  public boolean isParameterizedType() {
    return false;
  }

  public boolean isTypeVariable() {
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
