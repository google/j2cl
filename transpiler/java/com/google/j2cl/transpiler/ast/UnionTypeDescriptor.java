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
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
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
@Visitable
@AutoValue
public abstract class UnionTypeDescriptor extends TypeDescriptor {

  public abstract ImmutableList<TypeDescriptor> getUnionTypeDescriptors();

  @Override
  @Memoized
  public boolean isNullable() {
    return getUnionTypeDescriptors().stream().anyMatch(TypeDescriptor::isNullable);
  }

  @Override
  public DeclaredTypeDescriptor toRawTypeDescriptor() {
    return getClosestCommonSuperClass().toRawTypeDescriptor();
  }

  /** Returns the closest common super-type of all type descriptors in this union. */
  @Memoized
  public DeclaredTypeDescriptor getClosestCommonSuperClass() {
    DeclaredTypeDescriptor typeDescriptor =
        (DeclaredTypeDescriptor) getUnionTypeDescriptors().getFirst();
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
  @Nullable
  public MethodDescriptor getMethodDescriptor(String methodName, TypeDescriptor... parameters) {
    // There might be different methods in the different components of the union with different
    // parameterizations, so this method should return one with a parameterization that
    // consistent with all components. For this reason the method is not supported.
    throw new UnsupportedOperationException("getMethodDescriptor is unsupported in union types.");
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
  @Nullable
  public DeclaredTypeDescriptor findSupertype(TypeDeclaration supertypeDeclaration) {
    return getUnionTypeDescriptors().stream()
        .map(td -> td.findSupertype(supertypeDeclaration))
        // Perform a reduction where if any value is null, the result is null.
        // For union types, all types must have the given supertype in order to be considered.
        .reduce((a, b) -> (a == null || b == null) ? null : a)
        .orElse(null);
  }

  @Override
  boolean isDenotable(ImmutableSet<TypeVariable> seen) {
    return false;
  }

  @Override
  boolean hasReferenceTo(TypeVariable typeVariable, ImmutableSet<TypeVariable> seen) {
    return getUnionTypeDescriptors().stream().anyMatch(it -> it.hasReferenceTo(typeVariable, seen));
  }

  @Override
  String toStringInternal(ImmutableSet<TypeVariable> seen) {
    return getUnionTypeDescriptors().stream()
        .map(t -> t.toStringInternal(seen))
        .collect(joining(" | ", "(", ")"));
  }

  @Override
  TypeDescriptor acceptInternal(Processor processor) {
    return Visitor_UnionTypeDescriptor.visit(processor, this);
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
