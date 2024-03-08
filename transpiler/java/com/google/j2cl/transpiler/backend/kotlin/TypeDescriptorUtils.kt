/*
 * Copyright 2022 Google Inc.
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
package com.google.j2cl.transpiler.backend.kotlin

import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor
import com.google.j2cl.transpiler.ast.KtVariance
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangVoid
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor
import com.google.j2cl.transpiler.backend.kotlin.common.runIf
import kotlin.streams.asSequence

internal val TypeDescriptor.isImplicitUpperBound
  get() = this == nullableAnyTypeDescriptor

// TODO(b/216796920): Remove when the bug is fixed.
private val DeclaredTypeDescriptor.directlyDeclaredTypeArgumentDescriptors: List<TypeDescriptor>
  get() = typeArgumentDescriptors.take(typeDeclaration.directlyDeclaredTypeParameterCount)

internal fun DeclaredTypeDescriptor.directlyDeclaredNonRawTypeArgumentDescriptors(
  projectToWildcards: Boolean
): List<TypeDescriptor> =
  if (!isRaw) directlyDeclaredTypeArgumentDescriptors
  else
    projectToWildcards.or(typeDeclaration.hasRecursiveTypeBounds()).let { mapToWildcard ->
      typeDeclaration.directlyDeclaredTypeParameterDescriptors.map {
        if (mapToWildcard) {
          TypeVariable.createWildcard()
        } else {
          it.upperBoundTypeDescriptor.toRawTypeDescriptor()
        }
      }
    }

/** Returns direct super type to use for super method call. */
internal fun DeclaredTypeDescriptor.directSuperTypeForMethodCall(
  methodDescriptor: MethodDescriptor
): DeclaredTypeDescriptor? =
  superTypesStream
    .asSequence()
    // Skip java.lang.Object as a supertype of interfaces.
    .filter { superType -> superType.isInterface || !isInterface }
    .mapNotNull { superType ->
      // See if the method is in this supertype (in which case we are done) or if it is
      // overridden here (in which case this supertype is not the target).
      val declaredSuperMethodDescriptor =
        superType.declaredMethodDescriptors.find {
          it == methodDescriptor || it.isOverride(methodDescriptor)
        }
      when (declaredSuperMethodDescriptor) {
        // The method has not been found nor it is overridden in this supertype so continue looking
        // up the hierarchy; so if we find it up the hierarchy this is the supertype to return.
        null -> superType.takeIf { it.directSuperTypeForMethodCall(methodDescriptor) != null }
        // We found the implementation targeted, so return this supertype.
        methodDescriptor -> superType
        // We found an override of the method in the hierarchy, so this supertype is not providing
        // the implementation targeted.
        else -> null
      }
    }
    .firstOrNull()

internal fun TypeDescriptor.contains(
  typeVariable: TypeVariable,
  seenTypeVariables: Set<TypeVariable> = setOf(),
): Boolean =
  when (this) {
    is DeclaredTypeDescriptor ->
      typeArgumentDescriptors.any { it.contains(typeVariable, seenTypeVariables) }
    is IntersectionTypeDescriptor ->
      intersectionTypeDescriptors.any { it.contains(typeVariable, seenTypeVariables) }
    is ArrayTypeDescriptor ->
      componentTypeDescriptor?.contains(typeVariable, seenTypeVariables) ?: false
    is TypeVariable ->
      if (seenTypeVariables.contains(this)) false
      else
        this == typeVariable ||
          seenTypeVariables.plus(this).let { seenTypeVariablesPlusThis ->
            upperBoundTypeDescriptor.contains(typeVariable, seenTypeVariablesPlusThis) ||
              (lowerBoundTypeDescriptor?.contains(typeVariable, seenTypeVariablesPlusThis) ?: false)
          }
    else -> false
  }

internal val TypeDescriptor.isKtDenotableNonWildcard: Boolean
  get() =
    when (this) {
      is TypeVariable -> !isWildcard && isKtDenotable
      else -> isKtDenotable
    }

internal val TypeDescriptor.isKtDenotable: Boolean
  get() =
    when (this) {
      is IntersectionTypeDescriptor ->
        // Kotlin supports "T & Any" intersection.
        intersectionTypeDescriptors.let { intersections ->
          intersections.size == 2 &&
            intersections[0].let { it is TypeVariable && !it.isWildcardOrCapture } &&
            intersections[1] == anyTypeDescriptor
        }
      else -> isDenotable
    }

internal val TypeVariable.hasNullableBounds: Boolean
  get() = upperBoundTypeDescriptor.canBeNull() && hasNullableRecursiveBounds

internal val TypeVariable.hasNullableRecursiveBounds: Boolean
  get() = upperBoundTypeDescriptors.all { it.canBeNullableAsBound }

/** Whether this type can be nullable when declared as an upper bound. */
internal val TypeDescriptor.canBeNullableAsBound: Boolean
  get() = this !is DeclaredTypeDescriptor || typeDeclaration.canBeNullableAsBound

/** Returns a version of this type descriptor where {@code canBeNull()} returns false. */
internal fun TypeDescriptor.makeNonNull(): TypeDescriptor =
  if (!canBeNull()) this
  else
    when (this) {
      is DeclaredTypeDescriptor -> toNonNullable()
      is TypeVariable ->
        if (!isWildcardOrCapture) {
          // TODO(b/328541289): Here it should be just `toNonNullable()`, in fact the handing below
          // for wildcards and captures should also be done by `toNonNullable()`. The only
          // kotlin output specific piece is the handling of `*`.
          if (hasNullableBounds) {
            // Convert to {@code T & Any}
            IntersectionTypeDescriptor.newBuilder()
              .setIntersectionTypeDescriptors(
                listOf(withoutNullabilityAnnotations(), anyTypeDescriptor)
              )
              .build()
          } else {
            withoutNullabilityAnnotations()
          }
        } else if (upperBoundTypeDescriptor.isImplicitUpperBound) {
          // Ignore type variables which will be rendered as star (unbounded wildcard).
          this
        } else {
          TypeVariable.Builder.from(this)
            .setUpperBoundTypeDescriptorSupplier { upperBoundTypeDescriptor.makeNonNull() }
            // Set some unique ID to avoid conflict with other type variables.
            // TODO(b/246332093): Remove when the bug is fixed, and uniqueId reflects bounds
            // properly.
            .setUniqueKey(
              "<??>" +
                "+${upperBoundTypeDescriptor.makeNonNull().uniqueId}" +
                "-${lowerBoundTypeDescriptor?.uniqueId}"
            )
            .build()
        }
      is IntersectionTypeDescriptor ->
        IntersectionTypeDescriptor.newBuilder()
          .setIntersectionTypeDescriptors(intersectionTypeDescriptors + anyTypeDescriptor)
          .build()
      is UnionTypeDescriptor ->
        UnionTypeDescriptor.newBuilder()
          .setUnionTypeDescriptors(unionTypeDescriptors.map { it.makeNonNull() })
          .build()
      is PrimitiveTypeDescriptor -> toNonNullable()
      is ArrayTypeDescriptor -> toNonNullable()
      else -> error("Unhandled $this")
    }

private val nullableAnyTypeDescriptor: TypeDescriptor
  get() = typeDescriptors.javaLangObject

private val anyTypeDescriptor: TypeDescriptor
  get() = nullableAnyTypeDescriptor.toNonNullable()

internal fun TypeDescriptor.applyVariance(variance: KtVariance?) =
  if (this is TypeVariable) variableApplyVariance(variance) else this

private fun TypeVariable.variableApplyVariance(variance: KtVariance?) =
  if (!isWildcardOrCapture) this
  else
    when (variance) {
      KtVariance.IN -> lowerBoundTypeDescriptor ?: this
      KtVariance.OUT -> upperBoundTypeDescriptor.takeIf { !it.isImplicitUpperBound } ?: this
      else -> this
    }

internal val typeDescriptors
  get() = TypeDescriptors.get()

internal val TypeVariable.hasAmpersandAny: Boolean
  get() = isAnnotatedNonNullable && (isNullable || upperBoundTypeDescriptor.canBeNull())

internal val TypeDescriptor.variableHasAmpersandAny: Boolean
  get() = this is TypeVariable && hasAmpersandAny

internal val TypeDescriptor.withImplicitNullability
  get() = runIf(isJavaLangVoid(this)) { toNullable() }
