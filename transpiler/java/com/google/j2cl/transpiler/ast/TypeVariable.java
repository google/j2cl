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

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.Objects;
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
    TypeDescriptor boundTypeDescriptor = getUpperBoundTypeDescriptorFactory().get(this);
    return boundTypeDescriptor != null ? boundTypeDescriptor : TypeDescriptors.get().javaLangObject;
  }

  /**
   * References to some descriptors need to be deferred since type variables are immutable and the
   * reference graph might be cyclic.
   */
  public interface DescriptorFactory<T> {
    T get(TypeVariable typeVariable);
  }

  public abstract DescriptorFactory<TypeDescriptor> getUpperBoundTypeDescriptorFactory();

  @Nullable
  abstract String getUniqueKey();

  @Override
  public boolean isTypeVariable() {
    return true;
  }

  @Nullable
  public abstract TypeDescriptor getLowerBoundTypeDescriptor();

  @Override
  public boolean isNullable() {
    return isAnnotatedNullable();
  }

  @Override
  public boolean canBeNull() {
    // TODO(b/244319605): Review semantics of nullability for lower bounded type variables.
    return !isAnnotatedNonNullable()
        && (isAnnotatedNullable() || getUpperBoundTypeDescriptor().canBeNull());
  }

  public abstract NullabilityAnnotation getNullabilityAnnotation();

  public boolean isAnnotatedNonNullable() {
    return getNullabilityAnnotation() == NullabilityAnnotation.NOT_NULLABLE;
  }

  public boolean isAnnotatedNullable() {
    return getNullabilityAnnotation() == NullabilityAnnotation.NULLABLE;
  }

  @Override
  @Memoized
  public TypeVariable toNullable() {
    if (isAnnotatedNullable()) {
      return this;
    }
    return TypeVariable.Builder.from(this)
        .setNullabilityAnnotation(NullabilityAnnotation.NULLABLE)
        .build();
  }

  @Override
  @Memoized
  public TypeVariable toNonNullable() {
    if (!canBeNull()) {
      // If the type variable does not have a nullable bound then it does not (and should not) need
      // to be annotated with `NOT_NULLABLE`, since it can only be instantiated by non nullable
      // types.
      return this;
    }
    return TypeVariable.Builder.from(this)
        .setNullabilityAnnotation(NullabilityAnnotation.NOT_NULLABLE)
        .build();
  }

  /** Returns the type variable without any nullability annotation. */
  @Memoized
  public TypeVariable withoutNullabilityAnnotations() {
    if (getNullabilityAnnotation() == NullabilityAnnotation.NONE) {
      return this;
    }
    return TypeVariable.Builder.from(this)
        .setNullabilityAnnotation(NullabilityAnnotation.NONE)
        .build();
  }

  /** Returns the declaration version of the type variable. */
  public TypeVariable toDeclaration() {
    // For now we use the same class to represent type variable declarations and references. The
    // declaration of a type variable is the version without any nullability annotation.
    return withoutNullabilityAnnotations();
  }

  @Override
  @Nullable
  public MethodDescriptor getMethodDescriptor(String methodName, TypeDescriptor... parameters) {
    return getUpperBoundTypeDescriptor().getMethodDescriptor(methodName, parameters);
  }

  @Nullable
  public abstract KtVariance getKtVariance();

  @Override
  public boolean isAssignableTo(TypeDescriptor that) {
    return this.getUpperBoundTypeDescriptor().isAssignableTo(that);
  }

  @Override
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
          .setUpperBoundTypeDescriptorFactory(() -> newUpperBound)
          .setLowerBoundTypeDescriptor(newLowerBound)
          .setUniqueKey("<Auto>" + getUniqueId())
          .build();
    }
    return this;
  }

  @Override
  public Set<TypeVariable> getAllTypeVariables() {
    if (!isWildcardOrCapture()) {
      return ImmutableSet.of(toDeclaration());
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

      ImmutableSet<TypeVariable> newSeen =
          new ImmutableSet.Builder<TypeVariable>().addAll(seen).add(this).build();

      return toWildcard()
          .withRewrittenBounds(
              it -> it.specializeTypeVariables(replacementTypeArgumentByTypeVariable, newSeen));
    }

    TypeVariable declaration = toDeclaration();

    TypeDescriptor specializedTypeVariable =
        replacementTypeArgumentByTypeVariable.apply(declaration);

    if (declaration != specializedTypeVariable) {
      // The variable has been specialized, apply the nullability annotation if the type variable
      // reference was annotated.
      return specializedTypeVariable.withNullabilityAnnotation(getNullabilityAnnotation());
    }
    return this;
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
    String prefix;
    switch (getNullabilityAnnotation()) {
      case NOT_NULLABLE:
        prefix = "!";
        break;
      case NULLABLE:
        prefix = "?";
        break;
      default:
        prefix = "";
    }
    return prefix + getUniqueKey();
  }

  public final boolean hasRecursiveDefinition() {
    return getUpperBoundTypeDescriptor().hasReferenceTo(this, ImmutableSet.of());
  }

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_TypeVariable.Builder()
        .setWildcard(false)
        .setCapture(false)
        .setNullabilityAnnotation(NullabilityAnnotation.NONE);
  }

  /** Creates a wildcard type variable with a specific upper bound. */
  public static TypeVariable createWildcardWithUpperBound(TypeDescriptor bound) {
    return createWildcard(/* upperBound= */ bound, /* lowerBound= */ null);
  }

  /** Creates a wildcard type variable with a specific lower bound. */
  public static TypeVariable createWildcardWithLowerBound(TypeDescriptor bound) {
    return createWildcard(
        /* upperBound= */ TypeDescriptors.get().javaLangObject, /* lowerBound= */ bound);
  }

  /** Creates wildcard type variable with no bound. */
  public static TypeVariable createWildcard() {
    return createWildcardWithUpperBound(TypeDescriptors.get().javaLangObject);
  }

  private static TypeVariable createWildcard(
      TypeDescriptor upperBound, @Nullable TypeDescriptor lowerBound) {
    String upperBoundKey = "<??_^_>" + upperBound.getUniqueId();
    String lowerBoundKey = lowerBound == null ? "" : "<??_v_>" + lowerBound.getUniqueId();

    String name = "?";
    if (lowerBound != null) {
      name += " super " + lowerBound.getReadableDescription();
    } else if (!TypeDescriptors.isJavaLangObject(upperBound)) {
      name += " extends " + upperBound.getReadableDescription();
    }

    return TypeVariable.newBuilder()
        .setWildcard(true)
        .setNullabilityAnnotation(NullabilityAnnotation.NONE)
        .setUpperBoundTypeDescriptorFactory(() -> upperBound)
        .setLowerBoundTypeDescriptor(lowerBound)
        // Create an unique key that does not conflict with the keys used for other types nor for
        // type variables coming from JDT, which follow "<declaring_type>:<name>...".
        // {@see org.eclipse.jdt.core.BindingKey}.
        .setUniqueKey(upperBoundKey + lowerBoundKey)
        .setName(name)
        .build();
  }

  /** Returns wildcard with the same bounds . */
  public TypeVariable toWildcard() {
    if (isWildcard()) {
      return this;
    }
    return createWildcard(getUpperBoundTypeDescriptor(), getLowerBoundTypeDescriptor());
  }

  /**
   * Returns wildcard with bounds rewritten using the given function, or throws if this type
   * variable is not a wildcard.
   */
  public TypeVariable withRewrittenBounds(
      Function<? super TypeDescriptor, ? extends TypeDescriptor> fn) {
    return withRewrittenBounds(fn, fn);
  }

  /**
   * Returns wildcard with bounds rewritten using the given function, or throws if this type
   * variable is not a wildcard.
   */
  public TypeVariable withRewrittenBounds(
      Function<? super TypeDescriptor, ? extends TypeDescriptor> upperBoundFn,
      Function<? super TypeDescriptor, ? extends TypeDescriptor> lowerBoundFn) {
    checkArgument(isWildcard());
    TypeDescriptor upperBound = getUpperBoundTypeDescriptor();
    TypeDescriptor lowerBound = getLowerBoundTypeDescriptor();
    TypeDescriptor updatedUpperBound = upperBoundFn.apply(upperBound);
    TypeDescriptor updatedLowerBound = lowerBound != null ? lowerBoundFn.apply(lowerBound) : null;

    if (upperBound.equals(updatedUpperBound) && Objects.equals(lowerBound, updatedLowerBound)) {
      return this;
    }

    return createWildcard(updatedUpperBound, updatedLowerBound);
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

    public abstract Builder setUpperBoundTypeDescriptorFactory(
        DescriptorFactory<TypeDescriptor> boundTypeDescriptorFactory);

    public Builder setUpperBoundTypeDescriptorFactory(
        Supplier<TypeDescriptor> boundTypeDescriptorFactory) {
      return setUpperBoundTypeDescriptorFactory(self -> boundTypeDescriptorFactory.get());
    }

    public abstract Builder setUniqueKey(String uniqueKey);

    public abstract Builder setName(String name);

    public abstract Builder setWildcard(boolean isCapture);

    public abstract Builder setCapture(boolean isCapture);

    public abstract Builder setLowerBoundTypeDescriptor(@Nullable TypeDescriptor typeDescriptor);

    public abstract Builder setKtVariance(@Nullable KtVariance ktVariance);

    public abstract Builder setNullabilityAnnotation(NullabilityAnnotation nullabilityAnnotation);

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
