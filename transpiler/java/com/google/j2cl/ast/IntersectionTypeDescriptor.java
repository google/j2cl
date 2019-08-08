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

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.ast.processors.common.Processor;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * An intersection type.
 *
 * <p>Intersection types in Java arise from intersection casts like {@code (A&B&C)} and type
 * variable upper bounds like {@code <T extends A&B>}
 */
@AutoValue
@Visitable
public abstract class IntersectionTypeDescriptor extends TypeDescriptor {

  public abstract ImmutableList<DeclaredTypeDescriptor> getIntersectionTypeDescriptors();

  @Override
  @Memoized
  public boolean isNullable() {
    // TODO(b/68725640): remove nullability for parts where is not relevant like this one.
    return getIntersectionTypeDescriptors().stream().allMatch(TypeDescriptor::isNullable);
  }

  @Override
  @Memoized
  public DeclaredTypeDescriptor toRawTypeDescriptor() {
    return getFirstType().toRawTypeDescriptor();
  }

  /**
   * Returns the first type in the intersection.
   *
   * <p>Following the approach Java uses regarding erasure of intersection types, variables and
   * expression of intersection types are seen as being typed at the first component. J2cl inserts
   * the necessary casts when accessing members the other types in the intersection type.
   */
  @Memoized
  public DeclaredTypeDescriptor getFirstType() {
    return getIntersectionTypeDescriptors().get(0);
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_IntersectionTypeDescriptor.visit(processor, this);
  }

  @Override
  public boolean isIntersection() {
    return true;
  }

  @Override
  @Memoized
  public DeclaredTypeDescriptor getFunctionalInterface() {
    return getIntersectionTypeDescriptors()
        .stream()
        .filter(DeclaredTypeDescriptor::isFunctionalInterface)
        .findFirst()
        .orElse(null);
  }

  @Override
  @Nullable
  public TypeDeclaration getMetadataTypeDeclaration() {
    return toRawTypeDescriptor().getMetadataTypeDeclaration();
  }

  @Override
  @Memoized
  public IntersectionTypeDescriptor toUnparameterizedTypeDescriptor() {
    return newBuilder()
        .setIntersectionTypeDescriptors(
            TypeDescriptors.toUnparameterizedTypeDescriptors(getIntersectionTypeDescriptors()))
        .build();
  }

  @Override
  public boolean isAssignableTo(TypeDescriptor that) {
    return getIntersectionTypeDescriptors()
        .stream()
        .anyMatch(typeDescriptor -> typeDescriptor.isAssignableTo(that));
  }

  @Override
  @Memoized
  public Set<TypeVariable> getAllTypeVariables() {
    return getIntersectionTypeDescriptors()
        .stream()
        .map(TypeDescriptor::getAllTypeVariables)
        .flatMap(set -> set.stream())
        .collect(Collectors.toSet());
  }

  @Override
  @Memoized
  public String getUniqueId() {
    return synthesizeIntersectionName(TypeDescriptor::getUniqueId);
  }

  @Override
  @Memoized
  public String getReadableDescription() {
    return synthesizeIntersectionName(TypeDescriptor::getReadableDescription);
  }

  private String synthesizeIntersectionName(Function<DeclaredTypeDescriptor, String> nameFunction) {
    return getIntersectionTypeDescriptors().stream()
        .map(nameFunction)
        .collect(joining("&", "(", ")"));
  }

  @Override
  public IntersectionTypeDescriptor toNullable() {
    return this;
  }

  @Override
  public IntersectionTypeDescriptor toNonNullable() {
    return this;
  }

  @Override
  public boolean canBeReferencedExternally() {
    return getIntersectionTypeDescriptors()
        .stream()
        .anyMatch(TypeDescriptor::canBeReferencedExternally);
  }

  @Memoized
  @Override
  public Map<TypeVariable, TypeDescriptor> getSpecializedTypeArgumentByTypeParameters() {
    ImmutableMap.Builder<TypeVariable, TypeDescriptor> mapBuilder = ImmutableMap.builder();
    for (TypeDescriptor typeDescriptor : getIntersectionTypeDescriptors()) {
      mapBuilder.putAll(typeDescriptor.getSpecializedTypeArgumentByTypeParameters());
    }
    return mapBuilder.build();
  }

  @Override
  public IntersectionTypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable) {
    if (AstUtils.isIdentityFunction(replacementTypeArgumentByTypeVariable)) {
      return this;
    }

    return newBuilder()
        .setIntersectionTypeDescriptors(
            getIntersectionTypeDescriptors()
                .stream()
                .map(
                    typeDescriptor ->
                        (DeclaredTypeDescriptor)
                            typeDescriptor.specializeTypeVariables(
                                replacementTypeArgumentByTypeVariable))
                .collect(ImmutableList.toImmutableList()))
        .build();
  }

  public static Builder newBuilder() {
    return new AutoValue_IntersectionTypeDescriptor.Builder();
  }

  /** Builder for an IntersectionTypeDescriptor. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setIntersectionTypeDescriptors(
        Iterable<DeclaredTypeDescriptor> components);

    abstract IntersectionTypeDescriptor autoBuild();

    private static final ThreadLocalInterner<IntersectionTypeDescriptor> interner =
        new ThreadLocalInterner<>();

    public IntersectionTypeDescriptor build() {
      IntersectionTypeDescriptor typeDescriptor = autoBuild();
      checkState(typeDescriptor.getIntersectionTypeDescriptors().size() > 1);
      return interner.intern(typeDescriptor);
    }
  }
}
