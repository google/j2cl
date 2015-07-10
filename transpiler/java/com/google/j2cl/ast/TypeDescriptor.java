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

/**
 * Base class for type reference.
 */
@Visitable
public abstract class TypeDescriptor extends Node implements Comparable<TypeDescriptor> {
  public static final String VOID_TYPE_NAME = "void";
  public static final String INT_TYPE_NAME = "int";
  public static final String BOOLEAN_TYPE_NAME = "boolean";
  public static final String BYTE_TYPE_NAME = "byte";
  public static final String CHAR_TYPE_NAME = "char";
  public static final String DOUBLE_TYPE_NAME = "double";
  public static final String FLOAT_TYPE_NAME = "float";
  public static final String LONG_TYPE_NAME = "long";
  public static final String SHORT_TYPE_NAME = "short";

  public static final TypeDescriptor VOID_TYPE_DESCRIPTOR = createPrimitive(VOID_TYPE_NAME);
  public static final TypeDescriptor INT_TYPE_DESCRIPTOR = createPrimitive(INT_TYPE_NAME);
  public static final TypeDescriptor OBJECT_TYPE_DESCRIPTOR =
      RegularTypeDescriptor.create(
          Arrays.asList("java", "lang"), Arrays.asList("Object"), "Object");
  /**
   * Implements devirtualized Object methods that allow us to special case some behavior and treat
   * some native JS classes as the same as some matching Java classes.
   */
  public static final TypeDescriptor OBJECTS_TYPE_DESCRIPTOR =
      RegularTypeDescriptor.createRaw(Arrays.asList("vmbootstrap"), "Objects", "ObjectsModule");

  private static Interner<TypeDescriptor> interner;

  public static TypeDescriptor create(
      Iterable<String> packageComponents,
      Iterable<String> classComponents,
      String compilationUnitSimpleName) {
    Preconditions.checkArgument(!Iterables.getLast(classComponents).contains("<"));
    return getInterner()
        .intern(
            new AutoValue_RegularTypeDescriptor(
                ImmutableList.copyOf(packageComponents),
                ImmutableList.copyOf(classComponents),
                compilationUnitSimpleName,
                false));
  }

  public static TypeDescriptor createRaw(
      Iterable<String> nameSpaceComponents, String className, String moduleName) {
    return getInterner()
        .intern(
            new AutoValue_RegularTypeDescriptor(
                ImmutableList.copyOf(nameSpaceComponents),
                ImmutableList.of(className),
                moduleName,
                true));
  }

  static TypeDescriptor createPrimitive(String primitiveTypeName) {
    return create(new ArrayList<String>(), Arrays.asList(primitiveTypeName), "");
  }

  static Interner<TypeDescriptor> getInterner() {
    if (interner == null) {
      interner = Interners.newWeakInterner();
    }
    return interner;
  }

  public abstract String getBinaryName();

  public abstract String getClassName();

  public abstract String getCompilationUnitSourceName();

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

  public abstract int getDimensions();

  public abstract TypeDescriptor getLeafTypeDescriptor();

  @Override
  public int compareTo(TypeDescriptor that) {
    return getBinaryName().compareTo(that.getBinaryName());
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TypeDescriptor) {
      return getBinaryName().equals(((TypeDescriptor) o).getBinaryName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getBinaryName());
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_TypeDescriptor.visit(processor, this);
  }
}
