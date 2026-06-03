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

import com.google.j2cl.transpiler.ast.MemberDescriptor
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.backend.kotlin.ast.Visibility as KtVisibility

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
      // TODO(b/483489173): Remove when visibility problem in Dagger (fastinit) is solved
      // differently.
      isConstructor && hasInjectAnnotation -> KtVisibility.PUBLIC
      // When not translating actual visibilites, map protected to public, to allow access within
      // the same package across different types.
      isProtectedTranslatedAsPublic -> KtVisibility.PUBLIC
      // Overrides of protected methods translated as public needs to be public.
      this is MethodDescriptor && overridesProtectedAsPublic -> KtVisibility.PUBLIC
      // For all other cases, use the default visibility mapping.
      else -> KtVisibility.from(visibility)
    }

internal val MemberDescriptor.isProtectedTranslatedAsPublic: Boolean
  get() = !useActualKtVisibility && visibility.isProtected

internal val MethodDescriptor.overridesProtectedAsPublic: Boolean
  get() = javaOverriddenMethodDescriptors.any {
    it.isProtectedTranslatedAsPublic || it.overridesProtectedAsPublic
  }
