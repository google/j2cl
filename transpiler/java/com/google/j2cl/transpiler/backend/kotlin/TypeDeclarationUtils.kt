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

import com.google.j2cl.transpiler.ast.MemberDescriptor
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.ast.Visibility
import com.google.j2cl.transpiler.backend.kotlin.ast.Visibility as KtVisibility

// TODO(b/216796920): Remove when the bug is fixed.
internal val TypeDeclaration.directlyDeclaredTypeParameterDescriptors: List<TypeVariable>
  get() = typeParameterDescriptors.take(directlyDeclaredTypeParameterCount)

// TODO(b/216796920): Remove when the bug is fixed.
internal val TypeDeclaration.directlyDeclaredTypeParameterCount: Int
  get() {
    val enclosingInstanceTypeParameterCount =
      enclosingTypeDeclaration
        ?.takeIf { isCapturingEnclosingInstance }
        ?.typeParameterDescriptors
        ?.size ?: 0

    val enclosingMethodTypeParameterCount =
      enclosingMethodDescriptor?.typeParameterTypeDescriptors?.size ?: 0

    return typeParameterDescriptors.size
      .minus(enclosingInstanceTypeParameterCount)
      .minus(enclosingMethodTypeParameterCount)
  }

internal val TypeDeclaration.canBeNullableAsBound: Boolean
  get() =
    !hasRecursiveTypeBounds() ||
      typeParameterDescriptors.all { it.upperBoundTypeDescriptor.isNullable }

internal val TypeDeclaration.isKtInner: Boolean
  get() =
    enclosingTypeDeclaration != null &&
      kind == TypeDeclaration.Kind.CLASS &&
      isCapturingEnclosingInstance &&
      !isLocal

internal val TypeDeclaration.isOpen: Boolean
  get() = !isFinal && !isAnonymous

internal val MethodDescriptor.isOpen: Boolean
  get() =
    enclosingTypeDescriptor.typeDeclaration.isOpen &&
      !isFinal &&
      !isConstructor &&
      !isStatic &&
      !visibility.isPrivate

internal val Visibility.defaultMemberKtVisibility: KtVisibility
  get() =
    when (this) {
      Visibility.PUBLIC -> KtVisibility.PUBLIC
      // Map protected to public, to allow access within the same package across different types.
      Visibility.PROTECTED -> KtVisibility.PUBLIC
      // Map package-private to internal.
      Visibility.PACKAGE_PRIVATE -> KtVisibility.INTERNAL
      // Map private to internal, to allow access to members in the same file across different
      // types.
      Visibility.PRIVATE -> KtVisibility.INTERNAL
    }

internal val MemberDescriptor.isEnumConstructor: Boolean
  get() = enclosingTypeDescriptor.isEnum && isConstructor

internal val MemberDescriptor.isInterfaceMethod: Boolean
  get() = enclosingTypeDescriptor.isInterface

internal fun TypeDeclaration.equalsOrEnclosedIn(other: TypeDeclaration): Boolean =
  this == other || enclosingTypeDeclaration?.equalsOrEnclosedIn(other) ?: false
