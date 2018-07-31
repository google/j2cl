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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.ast.annotations.Visitable;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;

/** A usage-site reference to a type. */
@Visitable
public abstract class TypeDescriptor extends Node
    implements Comparable<TypeDescriptor>, HasReadableDescription, HasSimpleSourceName {

  /** Returns the simple binary name like "Outer$Inner". Used for file naming purposes. */
  public String getSimpleBinaryName() {
    return Joiner.on('$').join(getClassComponents());
  }

  /**
   * Returns the fully package qualified binary name like "com.google.common.Outer$Inner".
   *
   * <p>Used for generated class metadata (per JLS), file overview, file path, unique id calculation
   * and other similar scenarios.
   */
  public String getQualifiedBinaryName() {
    return getSimpleBinaryName();
  }

  /**
   * Returns the fully package qualified source name like "com.google.common.Outer.Inner". Used in
   * places where original name is useful (like aliasing, identifying the corressponding java type,
   * Debug/Error output, etc.
   */
  public String getQualifiedSourceName() {
    return Joiner.on(".").join(getClassComponents());
  }

  /**
   * Returns a list of Strings representing the current type's simple name and enclosing type simple
   * names. For example for "com.google.foo.Outer" the class components are ["Outer"] and for
   * "com.google.foo.Outer.Inner" the class components are ["Outer", "Inner"].
   */
  @Override
  public ImmutableList<String> getClassComponents() {
    return ImmutableList.of();
  }

  public boolean isJsFunctionImplementation() {
    return false;
  }

  public boolean isJsFunctionInterface() {
    return false;
  }

  public boolean isNative() {
    return false;
  }

  /** Return whether this type can be used directly by JavaScript code. */
  public abstract boolean canBeReferencedExternally();

  public boolean isPrimitive() {
    return false;
  }

  /** Returns whether the described type is a union. */
  public boolean isUnion() {
    return false;
  }

  /** Returns whether the described type is an array. */
  public boolean isArray() {
    return false;
  }

  /** Returns whether the described type is a class. */
  public boolean isClass() {
    return false;
  }

  /** Returns whether the described type is an interface. */
  public boolean isInterface() {
    return false;
  }

  /** Returns whether the described type is an enum type. */
  public boolean isEnum() {
    return false;
  }

  /** Returns whether the described type is an interface. */
  public boolean isIntersection() {
    return false;
  }

  /** Returns the type that holds the metadata for the class type */
  @Nullable
  public abstract TypeDeclaration getMetadataTypeDeclaration();

  /** Returns the functional interface implemented by the type */
  @Nullable
  public DeclaredTypeDescriptor getFunctionalInterface() {
    return null;
  }

  /** Returns the corresponding primitive type if the {@code setTypeDescriptor} is a boxed type. */
  public PrimitiveTypeDescriptor toUnboxedType() {
    return (PrimitiveTypeDescriptor) this;
  }

  /** Returns the value for uninitialized expression of this type. */
  public Expression getDefaultValue() {
    return NullLiteral.get();
  }

  /** Return whether this type is nullable or not. */
  public abstract boolean isNullable();

  /** Return a nullable version of this type descriptor if possible. */
  public abstract TypeDescriptor toNullable();

  /** Return a non nullable version of this type descriptor if possible. */
  public abstract TypeDescriptor toNonNullable();

  /** Returns type descriptor for the same type use the type parameters from the declaration. */
  public abstract TypeDescriptor toUnparameterizedTypeDescriptor();

  /**
   * Returns the erasure type (see definition of erasure type at
   * http://help.eclipse.org/luna/index.jsp) with an empty type arguments list.
   */
  @Nullable
  public abstract TypeDescriptor toRawTypeDescriptor();

  /** Returns all the free type variables that appear in the type. */
  public Set<TypeVariable> getAllTypeVariables() {
    return ImmutableSet.of();
  }

  /**
   * A mapping that fully describes the final specialized type argument value for every super type
   * or interface of the current type.
   *
   * <p>For example given:
   *
   * <pre>
   * class A<A1, A2> {}
   * class B<B1> extends A<String, B1>
   * class C<C1> extends B<C1>
   * </pre>
   *
   * <p>If the current type is C then the resulting mappings are:
   *
   * <pre>
   * - A1 -> String
   * - A2 -> C1
   * - B1 -> C1
   * </pre>
   */
  public abstract Map<TypeVariable, TypeDescriptor> getSpecializedTypeArgumentByTypeParameters();

  public TypeDescriptor specializeTypeVariables(
      Map<TypeVariable, TypeDescriptor> replacementTypeArgumentByTypeVariable) {
    return specializeTypeVariables(
        TypeDescriptors.mappingFunctionFromMap(replacementTypeArgumentByTypeVariable));
  }

  /** Replaces all occurrences of a type variable for the type specified by the mapping function. */
  public abstract TypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable);

  public final boolean hasSameRawType(TypeDescriptor other) {
    // TODO(rluble): compare using toRawTypeDescriptor once raw TypeDescriptors are constructed
    // correctly. Raw TypeDescriptors are constructed in one of two ways, 1) from a JDT RAW
    // TypeDescriptor and 2) from a TypeDescriptor by removing type variables. These two ways are
    // not consistent, in particular the second form does not propagate the removal of type
    // variables inward. These two construction end up with different data but with the same unique
    // id, so the first one that is constructed will be interned and used everywhere.
    // Using toRawTypeDescriptor here triggers the second (incorrect) construction and causes
    // the wrong information be used in some cases.

    // For type variables, wildcards and captures we still need to do toRawTypeDescriptor to get
    // the bound.
    TypeDescriptor thisTypeDescriptor = this instanceof TypeVariable ? toRawTypeDescriptor() : this;
    other = other instanceof TypeVariable ? other.toRawTypeDescriptor() : other;
    return thisTypeDescriptor.getQualifiedSourceName().equals(other.getQualifiedSourceName());
  }

  public boolean isAssignableTo(TypeDescriptor that) {
    return this == that;
  }

  /** A unique string for a give type. Used for interning. */
  public abstract String getUniqueId();

  @Override
  public final int compareTo(TypeDescriptor that) {
    return getUniqueId().compareTo(that.getUniqueId());
  }

  @Override
  public final boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o == this) {
      return true;
    }

    if (o instanceof TypeDescriptor) {
      return getUniqueId().equals(((TypeDescriptor) o).getUniqueId());
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return getUniqueId().hashCode();
  }

  @Override
  public final String toString() {
    return getUniqueId();
  }

  /** Returns a description that is useful for error messages. */
  @Override
  public String getReadableDescription() {
    return getSimpleSourceName();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_TypeDescriptor.visit(processor, this);
  }
}
