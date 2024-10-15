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
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.backend.kotlin.common.runIf

/**
 * Represents the mapping between a type variable and the type it takes in a parameterized
 * construct.
 *
 * <p>Use {@code DeclaredTypeDescriptor.typeArgumentTypeBindings(...)} and {@code
 * MethodDescriptor.typeArgumentTypeBindings} to get type arguments for type and method descriptors.
 *
 * <p>Use {@code Renderer.renderTypeBindings(...)} to render them.
 */
internal data class TypeBinding(
  val typeParameterDescriptor: TypeVariable,
  val typeArgumentDescriptor: TypeDescriptor,
)

private fun TypeBinding.makeNonNull() =
  copy(typeArgumentDescriptor = typeArgumentDescriptor.makeNonNull())

internal fun TypeBinding.toNonNullable() =
  copy(typeArgumentDescriptor = typeArgumentDescriptor.toNonNullable())

internal val TypeBinding.isDenotable
  get() = typeArgumentDescriptor.isDenotableNonWildcard

internal val ArrayTypeDescriptor.componentTypeBinding
  get() = typeBinding(arrayComponentTypeParameter, componentTypeDescriptor)

internal fun DeclaredTypeDescriptor.typeArgumentTypeBindings(
  projectRawToWildcards: Boolean = false
): List<TypeBinding> =
  typeDeclaration.typeParameterDescriptors
    .zip(toNonRawTypeDescriptor(projectRawToWildcards).typeArgumentDescriptors, ::typeBinding)
    .take(typeDeclaration.directlyDeclaredTypeParameterCount)

private fun DeclaredTypeDescriptor.toNonRawTypeDescriptor(
  projectRawToWildcards: Boolean = false
): DeclaredTypeDescriptor =
  runIf(isRaw) {
    declarationDescriptor.specializeRawTypeVariables(
      toWildcards = projectRawToWildcards || typeDeclaration.hasRecursiveTypeBounds()
    )
  }

private fun DeclaredTypeDescriptor.specializeRawTypeVariables(
  toWildcards: Boolean
): DeclaredTypeDescriptor = specializeTypeVariables {
  if (toWildcards) {
    TypeVariable.createWildcard()
  } else {
    (it.toRawTypeDescriptor() as DeclaredTypeDescriptor).specializeRawTypeVariables(toWildcards)
  }
}

internal val MethodDescriptor.typeArgumentTypeBindings: List<TypeBinding>
  get() =
    declarationDescriptor.typeParameterTypeDescriptors.zip(
      typeArgumentTypeDescriptors,
      ::typeBinding,
    )

private fun typeBinding(typeParameter: TypeVariable, typeDescriptor: TypeDescriptor) =
  TypeBinding(typeParameter, typeDescriptor.withImplicitNullability)
    .withInferredNullability
    .withFixedUnboundWildcard
    .updatedWithParameterVariance

private val TypeBinding.withInferredNullability: TypeBinding
  get() = runIf(!typeParameterDescriptor.hasNullableBounds) { makeNonNull() }

private val TypeBinding.updatedWithParameterVariance: TypeBinding
  get() =
    copy(
      typeArgumentDescriptor =
        typeArgumentDescriptor.applyVariance(typeParameterDescriptor.ktVariance)
    )

// TODO(b/245807463): Remove this fix when these bugs are fixed in the AST.
// TODO(b/255722110): Remove this fix when these bugs are fixed in the AST.
private val TypeBinding.withFixedUnboundWildcard: TypeBinding
  get() =
    runIf(isUnboundWildcardOrCapture) {
      copy(typeArgumentDescriptor = TypeVariable.createWildcard())
    }

// TODO(b/245807463): Remove this fix when these bugs are fixed in the AST.
// TODO(b/255722110): Remove this fix when these bugs are fixed in the AST.
private val TypeBinding.isUnboundWildcardOrCapture
  get() =
    typeArgumentDescriptor is TypeVariable &&
      typeArgumentDescriptor.isWildcardOrCapture &&
      typeArgumentDescriptor.lowerBoundTypeDescriptor == null &&
      typeArgumentDescriptor.upperBoundTypeDescriptor ==
        typeParameterDescriptor.upperBoundTypeDescriptor
