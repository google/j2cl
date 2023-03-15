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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * A union type.
 *
 * <p>Union types can only be introduced in Java in a {@code try {...} catch(...) {....}}
 * expression, when the exception variable is defined with the Java 8 multi-exception syntax e.g.
 *
 * <pre>
 * <code>
 *
 *  try {
 *     ...
 *   } catch (A | B e) {
 *     ...
 *   }
 * </code>
 * </pre>
 */
@AutoValue
public abstract class UnionTypeDescriptor extends TypeDescriptor {

  public abstract ImmutableList<TypeDescriptor> getUnionTypeDescriptors();

  @Override
  @Memoized
  public boolean isNullable() {
    return getUnionTypeDescriptors().stream().anyMatch(TypeDescriptor::isNullable);
  }

  @Override
  @Memoized
  public DeclaredTypeDescriptor toRawTypeDescriptor() {
    DeclaredTypeDescriptor typeDescriptor =
        (DeclaredTypeDescriptor) getUnionTypeDescriptors().get(0).toRawTypeDescriptor();
    // Find the closest common ancestor of all the types in the union.
    while (typeDescriptor != null && !isAssignableTo(typeDescriptor)) {
      typeDescriptor = typeDescriptor.getSuperTypeDescriptor();
    }
    return typeDescriptor == null ? TypeDescriptors.get().javaLangObject : typeDescriptor;
  }

  @Override
  @Nullable
  public TypeDeclaration getMetadataTypeDeclaration() {
    return null;
  }

  @Override
  public boolean isUnion() {
    return true;
  }

  @Override
  @Memoized
  public UnionTypeDescriptor toUnparameterizedTypeDescriptor() {
    return newBuilder()
        .setUnionTypeDescriptors(
            TypeDescriptors.toUnparameterizedTypeDescriptors(getUnionTypeDescriptors()))
        .build();
  }

  @Override
  public boolean isAssignableTo(TypeDescriptor that) {
    return getUnionTypeDescriptors()
        .stream()
        .allMatch(typeDescriptor -> typeDescriptor.isAssignableTo(that));
  }

  @Override
  @Memoized
  public Set<TypeVariable> getAllTypeVariables() {
    return getUnionTypeDescriptors()
        .stream()
        .map(TypeDescriptor::getAllTypeVariables)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  @Override
  @Memoized
  public String getUniqueId() {
    return synthesizeUnionName(TypeDescriptor::getUniqueId);
  }

  @Override
  @Memoized
  public String getReadableDescription() {
    return synthesizeUnionName(TypeDescriptor::getReadableDescription);
  }

  private String synthesizeUnionName(Function<TypeDescriptor, String> nameFunction) {
    return getUnionTypeDescriptors().stream().map(nameFunction).collect(joining("|", "(", ")"));
  }

  @Override
  public UnionTypeDescriptor toNullable() {
    if (isNullable()) {
      return this;
    }

    return UnionTypeDescriptor.newBuilder()
        .setUnionTypeDescriptors(
            getUnionTypeDescriptors().stream()
                .map(TypeDescriptor::toNullable)
                .collect(toImmutableList()))
        .build();
  }

  @Override
  public UnionTypeDescriptor toNonNullable() {
    if (!isNullable()) {
      return this;
    }

    return UnionTypeDescriptor.newBuilder()
        .setUnionTypeDescriptors(
            getUnionTypeDescriptors().stream()
                .map(TypeDescriptor::toNonNullable)
                .collect(toImmutableList()))
        .build();
  }

  @Override
  public boolean canBeReferencedExternally() {
    return false;
  }

  @Override
  TypeDescriptor replaceInternalTypeDescriptors(TypeReplacer fn, ImmutableSet<TypeVariable> seen) {
    ImmutableList<TypeDescriptor> unionTypes = getUnionTypeDescriptors();
    ImmutableList<TypeDescriptor> newUnionTypes = replaceTypeDescriptors(unionTypes, fn, seen);
    if (!unionTypes.equals(newUnionTypes)) {
      return UnionTypeDescriptor.newBuilder().setUnionTypeDescriptors(newUnionTypes).build();
    }
    return this;
  }

  @Override
  UnionTypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable,
      ImmutableSet<TypeVariable> seen) {
    if (AstUtils.isIdentityFunction(replacementTypeArgumentByTypeVariable)) {
      return this;
    }

    ImmutableList<TypeDescriptor> specializedUnionTypes =
        getUnionTypeDescriptors().stream()
            .map(
                typeDescriptor ->
                    typeDescriptor.specializeTypeVariables(
                        replacementTypeArgumentByTypeVariable, seen))
            .collect(ImmutableList.toImmutableList());

    return newBuilder().setUnionTypeDescriptors(specializedUnionTypes).build();
  }

  @Override
  public UnionTypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable) {
    return specializeTypeVariables(replacementTypeArgumentByTypeVariable, ImmutableSet.of());
  }

  @Override
  boolean isDenotable(ImmutableSet<TypeVariable> seen) {
    return false;
  }

  @Override
  boolean hasReferenceTo(TypeVariable typeVariable, ImmutableSet<TypeVariable> seen) {
    return getUnionTypeDescriptors().stream().anyMatch(it -> it.hasReferenceTo(typeVariable, seen));
  }

  public static Builder newBuilder() {
    return new AutoValue_UnionTypeDescriptor.Builder();
  }

  /** Builder for a UnionTypeDescriptor. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setUnionTypeDescriptors(Iterable<TypeDescriptor> components);

    abstract UnionTypeDescriptor autoBuild();

    private static final ThreadLocalInterner<UnionTypeDescriptor> interner =
        new ThreadLocalInterner<>();

    public UnionTypeDescriptor build() {
      return interner.intern(autoBuild());
    }
  }
}
