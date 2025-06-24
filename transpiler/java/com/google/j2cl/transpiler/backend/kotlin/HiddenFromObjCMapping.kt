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
package com.google.j2cl.transpiler.backend.kotlin

import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.FieldDescriptor
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.backend.kotlin.common.orIfNull

// TODO(b/407538927): Remove when no longer necessary.
internal class HiddenFromObjCMapping(
  // Mutable map from `TypeDeclaration` to `Boolean?` value indicating whether it requires
  // `@HiddenFromObjC` annotation, where `null` indicates that it's currently being processed and
  // used to detect recursion.
  private val typeDeclarationMap: MutableMap<TypeDeclaration, Boolean?> = mutableMapOf()
) {
  fun contains(typeDeclaration: TypeDeclaration): Boolean =
    if (typeDeclarationMap.containsKey(typeDeclaration)) {
      // null means recursion
      typeDeclarationMap[typeDeclaration].orIfNull { false }
    } else {
      typeDeclarationMap[typeDeclaration] = null
      val isHiddenFromObjC =
        HIDDEN_FROM_OBJC_TYPE_NAMES.contains(typeDeclaration.qualifiedSourceName) ||
          typeDeclaration.typeParameterDescriptors.any { contains(it) } ||
          typeDeclaration.superTypeDescriptor?.let { contains(it) }.orIfNull { false } ||
          typeDeclaration.interfaceTypeDescriptors.any { contains(it) } ||
          typeDeclaration.declaredMethodDescriptors.any { it.isConstructor && contains(it) }
      typeDeclarationMap[typeDeclaration] = isHiddenFromObjC
      isHiddenFromObjC
    }

  fun contains(methodDescriptor: MethodDescriptor): Boolean =
    when (methodDescriptor.enclosingTypeDescriptor) {
      // java.lang.String contains methods with StringBuilder, but these are marked as
      // @HiddenFromObjC in J2KT JRE, so it can be removed from this mapping.
      TypeDescriptors.get().javaLangString -> false
      else ->
        methodDescriptor.typeParameterTypeDescriptors.any { contains(it) } ||
          contains(methodDescriptor.returnTypeDescriptor) ||
          methodDescriptor.parameterTypeDescriptors.any { contains(it) }
    }

  fun contains(fieldDescriptor: FieldDescriptor): Boolean = contains(fieldDescriptor.typeDescriptor)

  fun contains(typeDescriptor: TypeDescriptor, seen: Set<TypeVariable> = setOf()): Boolean =
    when (typeDescriptor) {
      is PrimitiveTypeDescriptor -> false
      is ArrayTypeDescriptor -> contains(typeDescriptor.componentTypeDescriptor, seen)
      is DeclaredTypeDescriptor ->
        contains(typeDescriptor.typeDeclaration) ||
          typeDescriptor.typeArgumentDescriptors.any { contains(it, seen) }
      is TypeVariable ->
        if (seen.contains(typeDescriptor)) {
          false
        } else {
          seen.plus(typeDescriptor).let { seen ->
            contains(typeDescriptor.upperBoundTypeDescriptor, seen) ||
              typeDescriptor.lowerBoundTypeDescriptor?.let { contains(it, seen) }.orIfNull { false }
          }
        }
      else -> false
    }

  companion object {
    private val HIDDEN_FROM_OBJC_TYPE_NAMES = setOf("java.lang.Appendable")
  }
}
