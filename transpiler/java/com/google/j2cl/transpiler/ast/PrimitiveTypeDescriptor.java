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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;
import java.util.function.Function;

/** A primitive type. */
public class PrimitiveTypeDescriptor extends TypeDescriptor {
  private final String name;
  private final String signature;
  private final String boxedClassName;
  private final int precisionOrder;
  private final int width;

  public String getSimpleSourceName() {
    return name;
  }

  /** Returns the type signature that is used in JNI signatures. */
  public String getSignature() {
    return signature;
  }

  /** Returns the qualified source name of the corresponding boxed class. */
  public String getBoxedClassName() {
    return boxedClassName;
  }

  /* The width of the type in bits. */
  public int getWidth() {
    return width;
  }

  @Override
  public Expression getDefaultValue() {
    checkState(!TypeDescriptors.isPrimitiveVoid(this));
    if (TypeDescriptors.isPrimitiveBoolean(this)) {
      return BooleanLiteral.get(false);
    }
    return new NumberLiteral(this, 0);
  }

  @Override
  public String getReadableDescription() {
    return getSimpleSourceName();
  }

  @Override
  public String getMangledName() {
    return getSimpleSourceName();
  }

  @Override
  public boolean isNullable() {
    return false;
  }

  @Override
  public boolean isPrimitive() {
    return true;
  }

  /**
   * Returns true if this is wider than {@code thatType} from the arithmetic precision perspective.
   *
   * <p>Arithmetic operations with types of different widths require conversions.
   */
  public boolean isWiderThan(PrimitiveTypeDescriptor thatType) {
    checkArgument(isNumeric());
    checkArgument(thatType.isNumeric());
    return precisionOrder > thatType.precisionOrder;
  }

  private boolean isNumeric() {
    return precisionOrder > 0;
  }

  @Override
  public PrimitiveTypeDescriptor toRawTypeDescriptor() {
    return this;
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
  public PrimitiveTypeDescriptor toUnparameterizedTypeDescriptor() {
    return this;
  }

  @Override
  public DeclaredTypeDescriptor toBoxedType() {
    return TypeDescriptors.getBoxTypeFromPrimitiveType(this).toNonNullable();
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

    return toBoxedType().isAssignableTo(that);
  }

  @Override
  public boolean canBeReferencedExternally() {
    return true;
  }

  @Override
  public TypeDeclaration getMetadataTypeDeclaration() {
    return TypeDescriptors.createPrimitiveMetadataTypeDescriptor(this).getTypeDeclaration();
  }

  @Override
  TypeDescriptor replaceInternalTypeDescriptors(TypeReplacer fn, ImmutableSet<TypeVariable> seen) {
    return this;
  }

  @Override
  PrimitiveTypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable,
      ImmutableSet<TypeVariable> seen) {
    return this;
  }

  @Override
  public PrimitiveTypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable) {
    return specializeTypeVariables(replacementTypeArgumentByTypeVariable, ImmutableSet.of());
  }

  /** A unique string for a give type. Used for interning. */
  @Override
  public String getUniqueId() {
    return getSimpleSourceName();
  }

  @Override
  boolean isDenotable(ImmutableSet<TypeVariable> seen) {
    return true;
  }

  @Override
  boolean hasReferenceTo(TypeVariable typeVariable, ImmutableSet<TypeVariable> seen) {
    return false;
  }

  PrimitiveTypeDescriptor(
      String name, String signature, String boxedClassName, int precisionOrder, int width) {
    this.name = name;
    this.signature = signature;
    this.boxedClassName = boxedClassName;
    this.precisionOrder = precisionOrder;
    this.width = width;
  }
}
