/*
 * Copyright 2026 Google Inc.
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

import com.google.j2cl.transpiler.ast.FieldDescriptor
import com.google.j2cl.transpiler.ast.MemberDescriptor
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.backend.kotlin.ast.Visibility as KtVisibility
import com.google.j2cl.transpiler.backend.kotlin.ast.narrowDown
import com.google.j2cl.transpiler.backend.kotlin.ast.widenUp
import com.google.j2cl.transpiler.backend.kotlin.ast.withWidestScopeOrNull

internal val MemberDescriptor.isEnumConstructor: Boolean
  get() = enclosingTypeDescriptor.isEnum && isConstructor

internal val MemberDescriptor.isInterfaceMethod: Boolean
  get() = enclosingTypeDescriptor.isInterface

internal val MethodDescriptor.isOpen: Boolean
  get() =
    enclosingTypeDescriptor.typeDeclaration.isOpen &&
      !isFinal &&
      !isConstructor &&
      !isStatic &&
      !visibility.isPrivate

internal val MemberDescriptor.ktVisibility: KtVisibility
  get() =
    when {
      // Enum constructors are implicitly private in Kotlin
      isEnumConstructor -> KtVisibility.PRIVATE
      // All interface methods are public in Kotlin, and Java allows non-public static members, so
      // we map them to public.
      isInterfaceMethod -> KtVisibility.PUBLIC
      else ->
        // Use heuristics below to calculate a "base"visibility, then clamp down based on type
        // visibility, so more restricted type visibilites don't cause errors just because of
        // (presumbably accidental) wide method visibility.
        narrowDownVisibility(
          when {
            // TODO(b/483489173): Remove when visibility problem in Dagger (fastinit) is solved
            // differently.
            isConstructor && hasInjectAnnotation -> KtVisibility.PUBLIC
            // When not translating actual visibilites, map protected to public, to allow access
            // within the same package across different types.
            isProtectedTranslatedAsPublic -> KtVisibility.PUBLIC
            // For all other cases, use the default visibility mapping.
            else -> KtVisibility.from(visibility)
          }
        )
    }

internal val MemberDescriptor.isProtectedTranslatedAsPublic: Boolean
  get() = !useActualKtVisibility && visibility.isProtected

internal val MethodDescriptor.overridesProtectedAsPublic: Boolean
  get() = javaOverriddenMethodDescriptors.any {
    it.isProtectedTranslatedAsPublic || it.overridesProtectedAsPublic
  }

/**
 * Narrows down the visibility based on argument type visibility. The point is to simplyfy type
 * visibility restrictions. Kotlin forbids that members reference types that are more visible than
 * the method. So we automatically restrict method visibility accordingly, unless there are obvious
 * reasons why this won't work. Note that this will still not allow restricting type visibility to
 * match Java if something else relies on wider member visibility.
 */
private fun MemberDescriptor.narrowDownVisibility(baseVisibility: KtVisibility): KtVisibility {
  // Can't narrow down if it's already private.
  if (baseVisibility == KtVisibility.PRIVATE) {
    return KtVisibility.PRIVATE
  }
  val widestOverriddenVisibility = computeWidestOverriddenVisibility
  // Can't narrow down below widestOverridenVisibility
  if (
    widestOverriddenVisibility == KtVisibility.PUBLIC ||
      widestOverriddenVisibility == baseVisibility
  ) {
    return widestOverriddenVisibility
  }
  // TODO(b/206898384): The next line alone describes the behaviour; the lines above just skip
  // trivial cases for performance reasons.
  val narrowedVisibility: KtVisibility =
    baseVisibility.narrowDown(computeReferencedKtVisibilities).widenUp(widestOverriddenVisibility)

  // If the narrowed visibility is narrower than the enclosing type declaration, use it.
  // Otherwise, use the base visibility.
  return if (
    narrowedVisibility.hasNarrowerScopeThan(
      enclosingTypeDescriptor.typeDeclaration.inferredKtVisibility
    )
  ) {
    narrowedVisibility
  } else {
    baseVisibility
  }
}

val MemberDescriptor.computeReferencedKtVisibilities: List<KtVisibility>
  get() =
    when (this) {
        is MethodDescriptor ->
          parameterTypeDescriptors.map { it.inferredKtVisibility } +
            listOf(returnTypeDescriptor.inferredKtVisibility)
        is FieldDescriptor -> listOf(typeDescriptor.inferredKtVisibility)
        else -> emptyList()
      }
      // TODO(b/206898384): Where are PRIVATE cases coming from and why does code still compile
      // currently?
      .filterNotNull()
      .filter { it != KtVisibility.PRIVATE }

val MemberDescriptor.computeWidestOverriddenVisibility: KtVisibility?
  get() =
    if (this is MethodDescriptor) {
      javaOverriddenMethodDescriptors.map { it.ktVisibility }.withWidestScopeOrNull()
    } else {
      null
    }
