/*
 * Copyright 2025 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.annotations.FormatMethod;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor;
import com.google.j2cl.transpiler.ast.NullabilityAnnotation;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor;

/**
 * Abstract base class for Kotlin passes, providing shared functionality:
 *
 * <ul>
 *   <li>verbose type descriptions,
 * </ul>
 */
public abstract class AbstractJ2ktNormalizationPass extends NormalizationPass {
  // Prevent overriding and force using {@code applyTo(CompilationUnit)}.
  @Override
  public final void applyTo(Type type) {}

  @FormatMethod
  final void debug(SourcePosition sourcePosition, String detailMessage, Object... args) {
    getProblems().debug(sourcePosition, detailMessage, args);
  }

  @FormatMethod
  final void debug(String detailMessage, Object... args) {
    debug(SourcePosition.NONE, detailMessage, args);
  }

  /**
   * Converts captures to wildcards if they appear as a type argument, or project them to bounds if
   * they appear at the top-level (non as type argument).
   *
   * <ul>
   *   <li>{@code Foo<capture-of ? extends V>} -> {@code Foo<? extends V>}
   *   <li>{@code Foo<capture-of ? super V>} -> {@code Foo<? extends V>}
   *   <li>{@code capture-of ? extends V} -> {@code V}
   *   <li>{@code capture-of ? super V} -> {@code V}
   *   <li>{@code Foo<? extends V>} -> {@code Foo<? extends V>}
   *   <li>{@code Foo<? super V>} -> {@code Foo<? super V>}
   *   <li>{@code ? extends V} -> {@code V}
   *   <li>{@code ? super V} -> {@code V}
   * </ul>
   */
  static TypeDescriptor projectCaptures(TypeDescriptor typeDescriptor) {
    return projectCaptures(typeDescriptor, /* isTypeArgument= */ false, ImmutableSet.of());
  }

  private static TypeDescriptor projectCaptures(
      TypeDescriptor typeDescriptor, boolean isTypeArgument, ImmutableSet<TypeVariable> seen) {

    return switch (typeDescriptor) {
      case PrimitiveTypeDescriptor descriptor -> descriptor;

      case ArrayTypeDescriptor descriptor ->
          descriptor.withComponentTypeDescriptor(
              projectArgumentCaptures(
                  descriptor.getComponentTypeDescriptor(),
                  getArrayComponentTypeParameterDescriptor(),
                  seen));

      case DeclaredTypeDescriptor descriptor ->
          descriptor.withTypeArguments(
              Streams.zip(
                      descriptor.getTypeDeclaration().getTypeParameterDescriptors().stream(),
                      descriptor.getTypeArgumentDescriptors().stream(),
                      (typeParameter, typeArgument) ->
                          projectArgumentCaptures(typeArgument, typeParameter, seen))
                  .collect(toImmutableList()));

      case TypeVariable typeVariable when !typeVariable.isWildcardOrCapture() -> typeVariable;
      case TypeVariable typeVariable when seen.contains(typeVariable) -> typeVariable;
      case TypeVariable typeVariable -> {
        ImmutableSet<TypeVariable> newSeen =
            ImmutableSet.<TypeVariable>builder().addAll(seen).add(typeVariable).build();

        if (!isTypeArgument) {
          TypeDescriptor lowerBound =
              typeVariable.getLowerBoundTypeDescriptorWithAppliedNullability();
          if (lowerBound != null) {
            yield projectCaptures(lowerBound, /* isTypeArgument= */ false, newSeen);
          } else {
            TypeDescriptor upperBound =
                typeVariable.getUpperBoundTypeDescriptorWithAppliedNullability();
            yield projectCaptures(upperBound, /* isTypeArgument= */ false, newSeen);
          }
        } else {
          yield typeVariable
              .toWildcard()
              .withRewrittenBounds(it -> projectCaptures(it, /* isTypeArgument= */ false, newSeen));
        }
      }

      case IntersectionTypeDescriptor descriptor ->
          IntersectionTypeDescriptor.builder()
              .setIntersectionTypeDescriptors(
                  descriptor.getIntersectionTypeDescriptors().stream()
                      .map(it -> projectCaptures(it, /* isTypeArgument= */ false, seen))
                      .collect(toImmutableList()))
              .build();

      case UnionTypeDescriptor descriptor ->
          UnionTypeDescriptor.builder()
              .setUnionTypeDescriptors(
                  descriptor.getUnionTypeDescriptors().stream()
                      .map(it -> projectCaptures(it, /* isTypeArgument= */ false, seen))
                      .collect(toImmutableList()))
              .build();
    };
  }

  private static TypeDescriptor projectArgumentCaptures(
      TypeDescriptor argumentTypeDescriptor,
      TypeVariable typeParameter,
      ImmutableSet<TypeVariable> seen) {
    argumentTypeDescriptor =
        getTypeArgumentDescriptorWithValidNullability(argumentTypeDescriptor, typeParameter);

    // Don't project unbound wildcards with recursive declaration, as it'll result in recursive type
    // parameter appearing in the projection. Without this check, given the
    // {@code Foo<T extends Foo<T>>} declaration, the projection of {@code Foo<?>} would appear as
    // {@code Foo<Foo<T>>}.
    if (isUnboundWildcardWithRecursiveDeclaration(argumentTypeDescriptor, typeParameter)) {
      return argumentTypeDescriptor;
    }

    return argumentTypeDescriptor.isWildcardOrCapture()
        ? projectCaptures(argumentTypeDescriptor, /* isTypeArgument= */ true, seen)
        : argumentTypeDescriptor;
  }

  static boolean isUnboundWildcardWithRecursiveDeclaration(
      TypeDescriptor typeDescriptor, TypeVariable typeParameter) {
    return typeDescriptor instanceof TypeVariable typeVariable
        && typeParameter.hasRecursiveDefinition()
        && typeVariable.isWildcardOrCapture()
        && typeVariable
            .getUpperBoundTypeDescriptor()
            .toNullable()
            .equals(typeParameter.getUpperBoundTypeDescriptor().toNullable());
  }

  /** Returns synthetic type parameter for kotlin.Array class. */
  static TypeVariable getArrayComponentTypeParameterDescriptor() {
    return TypeVariable.builder()
        .setName("T")
        .setUniqueKey("kotlin.Array:T")
        .setUpperBoundTypeDescriptorFactory(() -> TypeDescriptors.get().javaLangObject)
        .build();
  }

  /** Returns type argument descriptors with nullability which satisfy their declarations. */
  static ImmutableList<TypeDescriptor> getTypeArgumentDescriptorsWithValidNullability(
      DeclaredTypeDescriptor declaredTypeDescriptor) {
    return Streams.zip(
            declaredTypeDescriptor.getTypeArgumentDescriptors().stream(),
            declaredTypeDescriptor.getTypeDeclaration().getTypeParameterDescriptors().stream(),
            AbstractJ2ktNormalizationPass::getTypeArgumentDescriptorWithValidNullability)
        .collect(toImmutableList());
  }

  /** Returns type argument descriptor with nullability which satisfies its declaration. */
  static TypeDescriptor getTypeArgumentDescriptorWithValidNullability(
      TypeDescriptor typeArgumentDescriptor, TypeVariable typeParameterDescriptor) {
    return !typeParameterDescriptor.canBeNull() && typeArgumentDescriptor.canBeNull()
        ? typeArgumentDescriptor.toNonNullable()
        : typeArgumentDescriptor;
  }

  /**
   * Returns type descriptor with removed redundant nullability annotation - in Kotlin, NOT_NULLABLE
   * is valid only for type variables with nullable bounds.
   */
  static TypeDescriptor removeRedundantNullabilityAnnotation(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof TypeVariable typeVariable) {
      if (typeVariable.getNullabilityAnnotation() == NullabilityAnnotation.NOT_NULLABLE
          && !typeVariable.getUpperBoundTypeDescriptor().canBeNull()) {
        return typeVariable.toBuilder()
            .setNullabilityAnnotation(NullabilityAnnotation.NONE)
            .build();
      }
    }
    return typeDescriptor;
  }
}
