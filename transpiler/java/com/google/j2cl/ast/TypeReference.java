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
public abstract class TypeReference extends Expression implements Comparable<TypeReference> {
  public static final String VOID_TYPE_NAME = "void";
  public static final String INT_TYPE_NAME = "int";

  public static final TypeReference VOID_TYPEREF = createPrimitive(VOID_TYPE_NAME);
  public static final TypeReference INT_TYPEREF = createPrimitive(INT_TYPE_NAME);
  public static final TypeReference OBJECT_TYPEREF =
      RegularTypeReference.create(Arrays.asList("java", "lang"), Arrays.asList("Object"), "Object");

  /**
   * Implements devirtualized Object methods that allow us to special case some behavior and treat
   * some native JS classes as the same as some matching Java classes.
   */
  public static final TypeReference OBJECTS_TYPEREF =
      RegularTypeReference.createRaw(Arrays.asList("vmbootstrap"), "Objects", "ObjectsModule");

  private static Interner<TypeReference> interner;

  public static TypeReference create(
      Iterable<String> packageComponents,
      Iterable<String> classComponents,
      String compilationUnitSimpleName) {
    Preconditions.checkArgument(!Iterables.getLast(classComponents).contains("<"));
    return getInterner()
        .intern(
            new AutoValue_RegularTypeReference(
                ImmutableList.copyOf(packageComponents),
                ImmutableList.copyOf(classComponents),
                compilationUnitSimpleName, false));
  }
  public static TypeReference createRaw(
      Iterable<String> nameSpaceComponents,
      String className,
      String moduleName) {
    return getInterner()
        .intern(
            new AutoValue_RegularTypeReference(
                ImmutableList.copyOf(nameSpaceComponents),
                ImmutableList.of(className),
                moduleName, true));
  }

  static TypeReference createPrimitive(String primitiveTypeName) {
    return create(new ArrayList<String>(), Arrays.asList(primitiveTypeName), "");
  }

  static Interner<TypeReference> getInterner() {
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
      case "boolean":
      case "byte":
      case "char":
      case "double":
      case "float":
      case INT_TYPE_NAME:
      case "long":
      case "short":
      case VOID_TYPE_NAME:
        return true;
      default:
        return false;
    }
  }

  public abstract int getDimensions();

  public abstract TypeReference getLeafTypeRef();

  @Override
  public int compareTo(TypeReference that) {
    return getBinaryName().compareTo(that.getBinaryName());
  }

  @Override
  public TypeReference accept(Processor processor) {
    return Visitor_TypeReference.visit(processor, this);
  }
}
