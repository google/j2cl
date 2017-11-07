/*
 * Copyright 2017 Google Inc.
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
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.Map;
import java.util.function.Function;

/** A primitive type. */
@AutoValue
@Visitable
public abstract class PrimitiveTypeDescriptor extends TypeDescriptor {

  @Override
  public abstract String getSimpleSourceName();

  @Override
  @Memoized
  public ImmutableList<String> getClassComponents() {
    return ImmutableList.of(getSimpleSourceName());
  }

  @Override
  @Memoized
  public boolean isNullable() {
    return false;
  }

  @Override
  @Memoized
  public PrimitiveTypeDescriptor getRawTypeDescriptor() {
    return this;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_PrimitiveTypeDescriptor.visit(processor, this);
  }

  @Override
  public boolean isPrimitive() {
    return true;
  }

  @Override
  @Memoized
  public PrimitiveTypeDescriptor unparameterizedTypeDescriptor() {
    return this;
  }

  /**
   * Returns {@code true} if this type is assignableTo the provided type.
   *
   * <p>The following is the assignability table between primitive types. The cell marked as 'X'
   * indicates that the type is assignable (and no cast nor conversion is needed).
   *
   * <p><code>
   * from\to       byte |  char | short | int   | long | float | double|
   * -------------------------------------------------------------------
   * byte        |  X   |       |   X   |   X   |      |   X   |   X   |
   * -------------------------------------------------------------------
   * char        |      |   X   |       |   X   |      |   X   |   X   |
   * -------------------------------------------------------------------
   * short       |      |       |   X   |   X   |      |   X   |   X   |
   * -------------------------------------------------------------------
   * int         |      |       |       |   X   |      |   X   |   X   |
   * -------------------------------------------------------------------
   * long        |      |       |       |       |   X  |       |       |
   * -------------------------------------------------------------------
   * float       |      |       |       |       |      |   X   |   X   |
   * -------------------------------------------------------------------
   * double      |      |       |       |       |      |   X   |   X   |
   * </code>
   */
  @Override
  @SuppressWarnings("ReferenceEquality")
  public boolean isAssignableTo(TypeDescriptor that) {
    if (this == that) {
      return true;
    }

    // Note that primitve longs are only assignable to/from primitive longs, so they are completely
    // absent in the following logic.
    if (TypeDescriptors.isPrimitiveByte(this)) {
      return TypeDescriptors.isPrimitiveShort(that)
          || TypeDescriptors.isPrimitiveInt(that)
          || TypeDescriptors.isPrimitiveFloatOrDouble(that);
    } else if (TypeDescriptors.isPrimitiveChar(this) || TypeDescriptors.isPrimitiveShort(this)) {
      return TypeDescriptors.isPrimitiveInt(that) || TypeDescriptors.isPrimitiveFloatOrDouble(that);
    } else if (TypeDescriptors.isPrimitiveInt(this)) {
      return TypeDescriptors.isPrimitiveFloatOrDouble(that);
    } else if (TypeDescriptors.isPrimitiveFloatOrDouble(this)) {
      // float and double will be represented both by the JavaScript type number,
      // they are assignable to each other which is slightly different from Java semantics.
      return TypeDescriptors.isPrimitiveFloatOrDouble(that);
    }

    return false;
  }

  /** A unique string for a give type. Used for interning. */
  @Override
  public String getUniqueId() {
    return getSimpleSourceName();
  }

  @Override
  public PrimitiveTypeDescriptor toNullable() {
    return this;
  }

  @Override
  public PrimitiveTypeDescriptor toNonNullable() {
    return this;
  }

  @Override
  public boolean canBeReferencedExternally() {
    return true;
  }

  @Override
  @Memoized
  public DeclaredTypeDescriptor getMetadataTypeDescriptor() {
    return TypeDescriptors.createPrimitiveMetadataTypeDescriptor(this);
  }

  @Memoized
  @Override
  public Map<TypeDescriptor, TypeDescriptor> getSpecializedTypeArgumentByTypeParameters() {
    return ImmutableMap.of();
  }

  @Override
  public PrimitiveTypeDescriptor specializeTypeVariables(
      Function<TypeDescriptor, TypeDescriptor> replacementTypeArgumentByTypeVariable) {
    return this;
  }

  @Override
  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_PrimitiveTypeDescriptor.Builder();
  }

  /** Builder for a TypeDescriptor. */
  @AutoValue.Builder
  public abstract static class Builder extends TypeDescriptor.Builder {

    public abstract Builder setSimpleSourceName(String name);

    abstract PrimitiveTypeDescriptor autoBuild();

    private static final ThreadLocalInterner<PrimitiveTypeDescriptor> interner =
        new ThreadLocalInterner<>();

    public PrimitiveTypeDescriptor build() {
      return interner.intern(autoBuild());
    }
  }
}
