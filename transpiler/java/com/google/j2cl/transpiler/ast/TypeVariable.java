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

import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * A definition-site or usage-site reference to a type variable.
 *
 * <p>Type variables are used to model both named variables and unnamed variables such as wildcards
 * and captures.
 *
 * <p>Some properties are lazily calculated since type relationships are cyclic graphs and the
 * TypeVariable class is a value type. Those properties are set through {@code Supplier}.
 */
@AutoValue
public abstract class TypeVariable extends TypeDescriptor implements HasName {

  @Override
  public abstract String getName();

  @Memoized
  public TypeDescriptor getUpperBoundTypeDescriptor() {
    TypeDescriptor boundTypeDescriptor = getUpperBoundTypeDescriptorSupplier().get();
    return boundTypeDescriptor != null ? boundTypeDescriptor : TypeDescriptors.get().javaLangObject;
  }

  public abstract Supplier<TypeDescriptor> getUpperBoundTypeDescriptorSupplier();

  @Nullable
  abstract String getUniqueKey();

  @Override
  public boolean isTypeVariable() {
    return true;
  }

  @Nullable
  public abstract TypeDescriptor getLowerBoundTypeDescriptor();

  @Override
  public abstract boolean isNullable();

  @Nullable
  public abstract KtVariance getKtVariance();

  public abstract boolean isAnnotatedNonNullable();

  @Override
  public TypeVariable toNullable() {
    if (isNullable()) {
      return this;
    }
    return TypeVariable.Builder.from(this).setNullable(true).setAnnotatedNonNullable(false).build();
  }

  @Override
  public TypeVariable toNonNullable() {
    if (!isNullable()) {
      return this;
    }
    return TypeVariable.Builder.from(this).setNullable(false).build();
  }

  @Override
  public boolean canBeNull() {
    // TODO(b/244319605): Review semantics of nullability for lower bounded type variables.
    return !isAnnotatedNonNullable() && (isNullable() || getUpperBoundTypeDescriptor().canBeNull());
  }

  @Override
  public boolean isAssignableTo(TypeDescriptor that) {
    return this.getUpperBoundTypeDescriptor().isAssignableTo(that);
  }

  /** Return true if it is an unnamed type variable, i.e. a wildcard or capture. */
  public final boolean isWildcardOrCapture() {
    return isWildcard() || isCapture();
  }

  /** Return true if it is a wildcard. */
  public abstract boolean isWildcard();

  /** Return true if it is a capture. */
  public abstract boolean isCapture();

  @Override
  public boolean isNoopCast() {
    return toRawTypeDescriptor().isNoopCast();
  }

  @Nullable
  @Override
  public TypeDeclaration getMetadataTypeDeclaration() {
    return getUpperBoundTypeDescriptor().getMetadataTypeDeclaration();
  }

  @Override
  public TypeDescriptor toRawTypeDescriptor() {
    return getUpperBoundTypeDescriptor().toRawTypeDescriptor();
  }

  @Override
  public PrimitiveTypeDescriptor toUnboxedType() {
    return toRawTypeDescriptor().toUnboxedType();
  }

  @Override
  public TypeDescriptor toUnparameterizedTypeDescriptor() {
    return this;
  }

  @Override
  public boolean canBeReferencedExternally() {
    return toRawTypeDescriptor().canBeReferencedExternally();
  }

  @Override
  TypeDescriptor replaceInternalTypeDescriptors(TypeReplacer fn, ImmutableSet<TypeVariable> seen) {
    // Avoid the recursion that might arise from type variable declarations,
    // (e.g. class Enum<T extends Enum<T>>).
    if (seen.contains(this)) {
      return this;
    }
    seen = new ImmutableSet.Builder<TypeVariable>().addAll(seen).add(this).build();
    TypeDescriptor upperBound = getUpperBoundTypeDescriptor();
    TypeDescriptor newUpperBound = replaceTypeDescriptors(upperBound, fn, seen);
    TypeDescriptor lowerBound = getLowerBoundTypeDescriptor();
    TypeDescriptor newLowerBound =
        lowerBound != null ? replaceTypeDescriptors(lowerBound, fn, seen) : null;
    if (upperBound != newUpperBound || lowerBound != newLowerBound) {
      return Builder.from(this)
          .setUpperBoundTypeDescriptorSupplier(() -> newUpperBound)
          .setLowerBoundTypeDescriptor(newLowerBound)
          .setUniqueKey("<Auto>" + getUniqueId())
          .build();
    }
    return this;
  }

  @Override
  public Set<TypeVariable> getAllTypeVariables() {
    if (!isWildcardOrCapture()) {
      return ImmutableSet.of(this.toNonNullable());
    }
    return ImmutableSet.of();
  }

  @Override
  TypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable,
      ImmutableSet<TypeVariable> seen) {
    if (isWildcardOrCapture()) {
      if (seen.contains(this)) {
        return this;
      }

      seen = new ImmutableSet.Builder<TypeVariable>().addAll(seen).add(this).build();

      TypeDescriptor upperBoundTypeDescriptor = getUpperBoundTypeDescriptor();
      TypeDescriptor lowerBoundTypeDescriptor = getLowerBoundTypeDescriptor();

      return createWildcardWithUpperAndLowerBound(
          upperBoundTypeDescriptor.specializeTypeVariables(
              replacementTypeArgumentByTypeVariable, seen),
          lowerBoundTypeDescriptor != null
              ? lowerBoundTypeDescriptor.specializeTypeVariables(
                  replacementTypeArgumentByTypeVariable, seen)
              : null);
    }

    TypeDescriptor specializedTypeVariable =
        replacementTypeArgumentByTypeVariable.apply(toNonNullable());
    // In our current model if the type variable that is specialized is not isNullable it means that
    // it does not have a @Nullable annotation, so we leave the specialized result alone, since
    // it might be nullable and needs to stay the same.
    return isNullable() ? specializedTypeVariable.toNullable() : specializedTypeVariable;
  }

  @Override
  public TypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable) {
    return specializeTypeVariables(replacementTypeArgumentByTypeVariable, ImmutableSet.of());
  }

  @Override
  public String getReadableDescription() {
    return getName();
  }

  @Override
  public String getUniqueId() {
    String prefix = isNullable() ? "?" : "!";
    String nonNullableAnnotationSuffix = isAnnotatedNonNullable() ? "&Any" : "";
    return prefix + getUniqueKey() + nonNullableAnnotationSuffix;
  }

  public final boolean hasRecursiveDefinition() {
    return getUpperBoundTypeDescriptor().hasReferenceTo(this, ImmutableSet.of());
  }

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_TypeVariable.Builder()
        .setWildcard(false)
        .setCapture(false)
        .setNullable(false)
        .setAnnotatedNonNullable(false);
  }

  /** Creates a wildcard type variable with a specific upper bound. */
  public static TypeVariable createWildcardWithUpperBound(TypeDescriptor bound) {
    return createWildcardWithUpperAndLowerBound(bound, null);
  }

  /** Creates a wildcard type variable with a specific lower bound. */
  public static TypeVariable createWildcardWithLowerBound(TypeDescriptor bound) {
    return createWildcardWithUpperAndLowerBound(TypeDescriptors.get().javaLangObject, bound);
  }

  /** Creates a wildcard type variable with a specific upper and lower bound. */
  public static TypeVariable createWildcardWithUpperAndLowerBound(
      TypeDescriptor upperBound, @Nullable TypeDescriptor lowerBound) {
    String upperBoundKey = "<??_^_>" + upperBound.getUniqueId();
    String lowerBoundKey = lowerBound == null ? "" : "<??_v_>" + lowerBound.getUniqueId();
    return TypeVariable.newBuilder()
        .setWildcard(true)
        .setNullable(false)
        .setUpperBoundTypeDescriptorSupplier(() -> upperBound)
        .setLowerBoundTypeDescriptor(lowerBound)
        // Create an unique key that does not conflict with the keys used for other types nor for
        // type variables coming from JDT, which follow "<declaring_type>:<name>...".
        // {@see org.eclipse.jdt.core.BindingKey}.
        .setUniqueKey(upperBoundKey + lowerBoundKey)
        .setName("?")
        .build();
  }

  /** Creates wildcard type variable with no bound. */
  public static TypeVariable createWildcard() {
    return createWildcardWithUpperBound(TypeDescriptors.get().javaLangObject);
  }

  @Override
  boolean isDenotable(ImmutableSet<TypeVariable> seen) {
    if (isCapture()) {
      return false;
    }

    if (!isWildcard()) {
      return true;
    }

    if (seen.contains(this)) {
      return true;
    }

    seen = ImmutableSet.<TypeVariable>builder().addAll(seen).add(this).build();

    TypeDescriptor lowerBound = getLowerBoundTypeDescriptor();
    if (lowerBound != null && !lowerBound.isDenotable(seen)) {
      return false;
    }

    TypeDescriptor upperBound = getUpperBoundTypeDescriptor();
    return upperBound.isDenotable(seen);
  }

  @Override
  boolean hasReferenceTo(TypeVariable typeVariable, ImmutableSet<TypeVariable> seen) {
    if (seen.contains(this)) {
      return false;
    }

    if (equals(typeVariable)) {
      return true;
    }

    seen = new ImmutableSet.Builder<TypeVariable>().addAll(seen).add(this).build();

    TypeDescriptor lowerBoundTypeDescriptor = getLowerBoundTypeDescriptor();
    if (lowerBoundTypeDescriptor != null
        && lowerBoundTypeDescriptor.hasReferenceTo(typeVariable, seen)) {
      return true;
    }

    return getUpperBoundTypeDescriptor().hasReferenceTo(typeVariable, seen);
  }

  /** Builder for a TypeVariableDeclaration. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setUpperBoundTypeDescriptorSupplier(
        Supplier<TypeDescriptor> boundTypeDescriptorFactory);

    public abstract Builder setUniqueKey(String uniqueKey);

    public abstract Builder setName(String name);

    public abstract Builder setWildcard(boolean isCapture);

    public abstract Builder setCapture(boolean isCapture);

    public abstract Builder setLowerBoundTypeDescriptor(@Nullable TypeDescriptor typeDescriptor);

    public abstract Builder setNullable(boolean isNullable);

    public abstract Builder setKtVariance(@Nullable KtVariance ktVariance);

    public abstract Builder setAnnotatedNonNullable(boolean hasNonNullAnnotation);

    private static final ThreadLocalInterner<TypeVariable> interner = new ThreadLocalInterner<>();

    abstract TypeVariable autoBuild();

    public TypeVariable build() {
      TypeVariable typeVariable = autoBuild();
      checkState(
          typeVariable.isWildcardOrCapture() || typeVariable.getLowerBoundTypeDescriptor() == null,
          "Only wildcard type variables can have lower bounds.");
      return interner.intern(typeVariable);
    }

    public static Builder from(TypeVariable typeVariable) {
      return typeVariable.toBuilder();
    }
  }
}
