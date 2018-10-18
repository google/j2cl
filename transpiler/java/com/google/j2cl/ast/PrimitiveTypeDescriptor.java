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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.ast.annotations.Visitable;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.concurrent.Immutable;

/** A primitive type. */
@Visitable
@Immutable
public class PrimitiveTypeDescriptor extends TypeDescriptor {
  private final String name;
  private final String boxedClassName;
  private final int precisionOrder;

  @Override
  public String getSimpleSourceName() {
    return name;
  }

  /** Returns the qualified source name of the corresponding boxed class. */
  public String getBoxedClassName() {
    return boxedClassName;
  }

  @Override
  public ImmutableList<String> getClassComponents() {
    return ImmutableList.of(getSimpleSourceName());
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
  public boolean isNullable() {
    return false;
  }

  @Override
  public boolean isPrimitive() {
    return true;
  }

  /**
   * Returns true if {@code thisType} is wider than {@code thatType} from the arithmetic precision
   * perspective.
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

  /** Returns the wider type descriptor, {@code this} if both have the same width */
  public PrimitiveTypeDescriptor widerTypeDescriptor(PrimitiveTypeDescriptor thatTypeDescriptor) {
    return thatTypeDescriptor.isWiderThan(this) ? thatTypeDescriptor : this;
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
    return checkNotNull(TypeDescriptors.getBoxTypeFromPrimitiveType(this));
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

  @Override
  public boolean canBeReferencedExternally() {
    return true;
  }

  @Override
  public TypeDeclaration getMetadataTypeDeclaration() {
    return TypeDescriptors.createPrimitiveMetadataTypeDescriptor(this).getTypeDeclaration();
  }

  @Override
  public Map<TypeVariable, TypeDescriptor> getSpecializedTypeArgumentByTypeParameters() {
    return ImmutableMap.of();
  }

  @Override
  public PrimitiveTypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable) {
    return this;
  }

  /** A unique string for a give type. Used for interning. */
  @Override
  public String getUniqueId() {
    return getSimpleSourceName();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_PrimitiveTypeDescriptor.visit(processor, this);
  }

  PrimitiveTypeDescriptor(String name, String boxedClassName, int precisionOrder) {
    this.name = name;
    this.boxedClassName = boxedClassName;
    this.precisionOrder = precisionOrder;
  }
}
