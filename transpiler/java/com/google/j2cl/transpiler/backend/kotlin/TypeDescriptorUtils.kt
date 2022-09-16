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
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor

internal val TypeDescriptor.isImplicitUpperBound
  get() = isJavaLangObject(this) && isNullable

// TODO(b/216796920): Remove when the bug is fixed.
private val DeclaredTypeDescriptor.directlyDeclaredTypeArgumentDescriptors: List<TypeDescriptor>
  get() = typeArgumentDescriptors.take(typeDeclaration.directlyDeclaredTypeParameterCount)

internal fun DeclaredTypeDescriptor.directlyDeclaredNonRawTypeArgumentDescriptors(
  projectToWildcards: Boolean
): List<TypeDescriptor> =
  if (!isRaw) directlyDeclaredTypeArgumentDescriptors
  else
    projectToWildcards.or(typeDeclaration.isRecursive).let { mapToWildcard ->
      typeDeclaration.directlyDeclaredTypeParameterDescriptors.map {
        if (mapToWildcard) TypeVariable.createWildcard() else it.upperBoundTypeDescriptor
      }
    }

/** Returns direct super type to use for super method call. */
internal fun DeclaredTypeDescriptor.directSuperTypeForMethodCall(
  methodDescriptor: MethodDescriptor
): DeclaredTypeDescriptor? =
  superTypesStream
    .map { superType ->
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
    .filter { it != null }
    .findFirst()
    .orElse(null)

internal fun TypeDescriptor.contains(typeVariable: TypeVariable): Boolean =
  when (this) {
    is DeclaredTypeDescriptor -> typeArgumentDescriptors.any { it.contains(typeVariable) }
    is TypeVariable -> this == typeVariable
    else -> false
  }

/** Returns whether this type is denotable as a top-level type of type argument. */
internal val TypeDescriptor.isDenotable
  get() =
    when (this) {
      is DeclaredTypeDescriptor -> !typeDeclaration.isAnonymous
      is TypeVariable -> !isWildcardOrCapture
      is IntersectionTypeDescriptor -> false
      is UnionTypeDescriptor -> false
      is PrimitiveTypeDescriptor -> true
      is ArrayTypeDescriptor -> true
      else -> error("Unhandled $this")
    }

internal fun inferNonNullableBounds(
  typeParameters: List<TypeVariable>,
  typeArguments: List<TypeDescriptor>
): List<TypeDescriptor> = typeParameters.zip(typeArguments, ::inferNonNullableBounds)

internal fun inferNonNullableBounds(
  typeParameter: TypeVariable,
  typeArgument: TypeDescriptor
): TypeDescriptor =
  if (!typeParameter.upperBoundTypeDescriptor.isNullable) typeArgument.toNonNullable()
  else typeArgument
