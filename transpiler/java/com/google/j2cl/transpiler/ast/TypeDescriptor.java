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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;

/** A usage-site reference to a type. */
public abstract class TypeDescriptor implements Comparable<TypeDescriptor>, HasReadableDescription {

  public boolean isJsType() {
    return false;
  }

  public boolean isJsFunctionImplementation() {
    return false;
  }

  public boolean isJsFunctionInterface() {
    return false;
  }

  public boolean isJsEnum() {
    return false;
  }

  /**
   * Returns the correspoinding {@link JsEnumInfo} if the type is a {@link
   * jsinterop.annotations.JsEnum} otherwise {@code null}
   */
  public JsEnumInfo getJsEnumInfo() {
    return null;
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

  /** Returns whether the described type is an array of primitive type. */
  public boolean isPrimitiveArray() {
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

  /** Returns whether the described type is a type variable. */
  public boolean isTypeVariable() {
    return false;
  }

  /** Returns whether the described type is a functional interface (JLS 9.8). */
  public boolean isFunctionalInterface() {
    return false;
  }

  /** Returns whether the described type has the @FunctionalInterface annotation. */
  public boolean isAnnotatedWithFunctionalInterface() {
    return false;
  }

  /**
   * Returns the mangled name of a type.
   *
   * <p>The mangled name of a type is a string that uniquely identifies the type and will become
   * part of the JavaScript method name to be able to differentiate method overloads.
   */
  public String getMangledName() {
    // Some type descriptors don't have a mangled name. Mangled names are only needed from
    // the "raw" type descriptors, i.e. declared types, primitives and arrays.
    throw new UnsupportedOperationException();
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

  /**
   * Returns the corresponding reference type if the {@code setTypeDescriptor} is a primitive type.
   */
  public DeclaredTypeDescriptor toBoxedType() {
    return (DeclaredTypeDescriptor) this;
  }

  /** Returns the value for uninitialized expression of this type. */
  public Expression getDefaultValue() {
    return getNullValue();
  }

  /** Returns a null literal value with this specific type. */
  public Expression getNullValue() {
    checkState(!isPrimitive());
    return NullLiteral.get(this);
  }

  /** Return whether this type is nullable or not. */
  public abstract boolean isNullable();

  /** Return a nullable version of this type descriptor if possible. */
  public abstract TypeDescriptor toNullable();

  /** Return a version of this type descriptor with the given nullability. */
  public final TypeDescriptor toNullable(boolean isNullable) {
    return isNullable ? toNullable() : toNonNullable();
  }

  /** Return a non nullable version of this type descriptor if possible. */
  public abstract TypeDescriptor toNonNullable();

  /** Return whether the value of this type can be null. */
  public boolean canBeNull() {
    return isNullable();
  }

  /** Returns type descriptor for the same type use the type parameters from the declaration. */
  public abstract TypeDescriptor toUnparameterizedTypeDescriptor();

  /**
   * Returns the erasure type (see definition of erasure type at
   * http://help.eclipse.org/luna/index.jsp) with an empty type arguments list.
   */
  public abstract TypeDescriptor toRawTypeDescriptor();

  /** Returns whether this is a raw type. */
  public boolean isRaw() {
    return false;
  }

  /**
   * Returns a reference to the JavaScript constructor to be used for array marking, instanceof and
   * casts. In most cases it the underlying JavaScript constructor for the class but not in all
   * (such as native @JsTypes and @JsFunctions).
   */
  public final JavaScriptConstructorReference getMetadataConstructorReference() {
    return new JavaScriptConstructorReference(getMetadataTypeDeclaration());
  }

  /** A function that replaces a TypeDescriptor. */
  public interface TypeReplacer {
    <T extends TypeDescriptor> T apply(T t);
  }

  /** Replaces all occurrences of a type for the type specified by the mapping function. */
  public static <T extends TypeDescriptor> T replaceTypeDescriptors(T t, TypeReplacer fn) {
    return replaceTypeDescriptors(t, fn, ImmutableSet.of());
  }

  static <T extends TypeDescriptor> T replaceTypeDescriptors(
      T t, TypeReplacer fn, ImmutableSet<TypeVariable> seen) {
    if (t == null) {
      return null;
    }
    T typeDescriptor = fn.apply(t);
    // Note that the use of generics is sketchy here. 'T' here is actually intendeted to be the
    // "this" type. As long as TypeReplacer guarantees preservation of type during replacement based
    // on its T -> T contract, we should be able to preserve 'this' type. However there is no way to
    // represent that through return here via Java generics without overhauling TypeDescriptor type
    // to have generics.
    @SuppressWarnings("unchecked")
    T replacement = (T) typeDescriptor.replaceInternalTypeDescriptors(fn, seen);
    return replacement;
  }

  /** Replaces all occurrences of a type for the type specified by the mapping function. */
  public static <T extends TypeDescriptor> ImmutableList<T> replaceTypeDescriptors(
      List<T> descriptors, TypeReplacer fn) {
    return replaceTypeDescriptors(descriptors, fn, ImmutableSet.of());
  }

  static <T extends TypeDescriptor> ImmutableList<T> replaceTypeDescriptors(
      List<T> descriptors, TypeReplacer fn, ImmutableSet<TypeVariable> seen) {
    return descriptors.stream()
        .map(t -> replaceTypeDescriptors(t, fn, seen))
        .collect(toImmutableList());
  }

  abstract TypeDescriptor replaceInternalTypeDescriptors(
      TypeReplacer fn, ImmutableSet<TypeVariable> seen);

  /** Returns all the free type variables that appear in the type. */
  public Set<TypeVariable> getAllTypeVariables() {
    return ImmutableSet.of();
  }

  public TypeDescriptor specializeTypeVariables(
      Map<TypeVariable, TypeDescriptor> replacementTypeArgumentByTypeVariable) {
    return specializeTypeVariables(
        TypeDescriptors.mappingFunctionFromMap(replacementTypeArgumentByTypeVariable));
  }

  /** Replaces all occurrences of a type variable for the type specified by the mapping function. */
  abstract TypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable,
      ImmutableSet<TypeVariable> seen);

  /** Replaces all occurrences of a type variable for the type specified by the mapping function. */
  public abstract TypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable);

  /**
   * Returns true if the two types have the same raw type.
   *
   * <p>The raw type is always an unparameterized (nullable) declared type or a primitive type. And
   * is defined as follows:
   * <li>If the type is a primitive type "{@code p}"-> then its raw type is itself, "{@code p}".
   * <li>If the type is a class, interface or enum "{@code !C<String>}" -> then its raw type is the
   *     (nullable) declared type with no parameterization, "{@code C}"
   * <li>If the type is an array type "{@code A[]}" -> its raw type is an array of the same
   *     dimensions but whose leaf type is the raw type of the leaf type of the original array.
   * <li>if the type is an intersection type "{@code (A<T> & B<U>)}" -> then its raw type is the raw
   *     type of the first component, "{@code A}".
   * <li>if the type is a type variable "{@code <T extends A>}"-> then its raw type is the raw type
   *     of its upper bound.
   * <li>if the types in an union type "{@code (A | B)}" -> then its raw type is the closest common
   *     supertype.
   */
  public final boolean hasSameRawType(TypeDescriptor other) {
    return toRawTypeDescriptor().isSameBaseType(other.toRawTypeDescriptor());
  }

  /**
   * For primitives, classes, interfaces, enums and arrays of those isSameBaseType returns {@code
   * true} if they have the same raw type. For all others it is only {@code true} if both types are
   * the same.
   */
  public boolean isSameBaseType(TypeDescriptor other) {
    return equals(other);
  }

  public boolean isAssignableTo(TypeDescriptor that) {
    return this == that;
  }

  /** Whether casts to this type are checked at runtime. */
  public boolean isNoopCast() {
    return false;
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

  public final boolean isDenotable() {
    return isDenotable(/* seen= */ ImmutableSet.of());
  }

  abstract boolean isDenotable(ImmutableSet<TypeVariable> seen);

  /**
   * Returns true if the definition of this type variable as a reference to {@code typeVariable}.
   */
  abstract boolean hasReferenceTo(TypeVariable typeVariable, ImmutableSet<TypeVariable> seen);
}
