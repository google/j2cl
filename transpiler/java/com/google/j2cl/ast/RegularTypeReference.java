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
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.processors.Visitable;

import java.util.Collections;

import javax.annotation.Nullable;

/**
 * A (by name) reference to a class.
 */
@AutoValue
@Visitable
public abstract class RegularTypeReference extends TypeReference {
  private static Interner<TypeReference> interner;

  public static TypeReference create(
      Iterable<String> packageComponents,
      Iterable<String> classComponents,
      String compilationUnitSimpleName) {
    return getInterner()
        .intern(
            new AutoValue_RegularTypeReference(
                ImmutableList.copyOf(packageComponents),
                ImmutableList.copyOf(classComponents),
                compilationUnitSimpleName));
  }

  public abstract ImmutableList<String> getPackageComponents();

  public abstract ImmutableList<String> getClassComponents();

  @Nullable
  public abstract String getCompilationUnitSimpleName();

  @Override
  public String getBinaryName() {
    return Joiner.on(".")
        .join(
            Iterables.concat(
                getPackageComponents(),
                Collections.singleton(Joiner.on("$").join(getClassComponents()))));
  }

  @Override
  public String getClassName() {
    return Joiner.on('$').join(getClassComponents());
  }

  @Override
  public String getCompilationUnitSourceName() {
    if (getCompilationUnitSimpleName() == null) {
      // Primitive type references don't have a compilation unit.
      return null;
    }
    return Joiner.on(".").join(getPackageComponents()) + "." + getCompilationUnitSimpleName();
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
  public String getPackageName() {
    return Joiner.on(".").join(getPackageComponents());
  }

  public TypeReference getArray(int dimensions) {
    return getInterner().intern(new AutoValue_ArrayTypeReference(dimensions, this));
  }

  @Override
  public boolean isArray() {
    return false;
  }

  @Override
  public int getDimensions() {
    return 0;
  }

  @Override
  public TypeReference getLeafTypeRef() {
    return null;
  }

  @Override
  public RegularTypeReference accept(Processor processor) {
    return Visitor_RegularTypeReference.visit(processor, this);
  }

  public boolean isPrimitive() {
    switch (getSourceName()) {
      case "boolean":
      case "byte":
      case "char":
      case "double":
      case "float":
      case "int":
      case "long":
      case "short":
      case "void":
        return true;
      default:
        return false;
    }
  }

  private static Interner<TypeReference> getInterner() {
    if (interner == null) {
      interner = Interners.newWeakInterner();
    }
    return interner;
  }
}
