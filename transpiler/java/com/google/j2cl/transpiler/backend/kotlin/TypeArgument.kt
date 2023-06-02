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

/**
 * Represents the mapping between a type variable and the type it takes in a parameterized
 * construct.
 *
 * <p>Use {@code DeclaredTypeDescriptor.typeArguments(...)} and {@code
 * MethodDescriptor.typeArguments} to get type arguments for type and method descriptors.
 *
 * <p>Use {@code Renderer.renderTypeArguments(...)} to render them.
 */
internal data class TypeArgument(
  val declarationTypeVariable: TypeVariable,
  val typeDescriptor: TypeDescriptor
)

private fun TypeArgument.makeNonNull() = copy(typeDescriptor = typeDescriptor.makeNonNull())

internal fun TypeArgument.toNonNullable() = copy(typeDescriptor = typeDescriptor.toNonNullable())

internal val TypeArgument.isDenotable
  get() = typeDescriptor.isKtDenotableNonWildcard

internal val ArrayTypeDescriptor.typeArgument
  get() = typeArgument(TypeVariable.createWildcard(), componentTypeDescriptor)

internal fun DeclaredTypeDescriptor.typeArguments(
  projectRawToWildcards: Boolean = false
): List<TypeArgument> =
  typeDeclaration.directlyDeclaredTypeParameterDescriptors.zip(
    directlyDeclaredNonRawTypeArgumentDescriptors(projectToWildcards = projectRawToWildcards),
    ::typeArgument
  )

internal val MethodDescriptor.typeArguments: List<TypeArgument>
  get() =
    declarationDescriptor.typeParameterTypeDescriptors.zip(
      typeArgumentTypeDescriptors,
      ::typeArgument
    )

private fun typeArgument(declarationTypeParameter: TypeVariable, typeDescriptor: TypeDescriptor) =
  TypeArgument(declarationTypeParameter, typeDescriptor.withImplicitNullability)
    .withFixedUnboundWildcard
    .withInferredNullability
    .updatedWithParameterVariance

private val TypeArgument.withInferredNullability: TypeArgument
  get() = if (!declarationTypeVariable.hasNullableBounds) makeNonNull() else this

private val TypeArgument.updatedWithParameterVariance: TypeArgument
  get() = copy(typeDescriptor = typeDescriptor.applyVariance(declarationTypeVariable.ktVariance))

// TODO(b/245807463): Remove this fix when these bugs are fixed in the AST.
// TODO(b/255722110): Remove this fix when these bugs are fixed in the AST.
private val TypeArgument.withFixedUnboundWildcard: TypeArgument
  get() =
    if (needsFixForUnboundWildcard) copy(typeDescriptor = TypeVariable.createWildcard()) else this

// TODO(b/245807463): Remove this fix when these bugs are fixed in the AST.
// TODO(b/255722110): Remove this fix when these bugs are fixed in the AST.
private val TypeArgument.needsFixForUnboundWildcard
  get() =
    typeDescriptor is TypeVariable &&
      typeDescriptor.isWildcardOrCapture &&
      typeDescriptor.lowerBoundTypeDescriptor == null &&
      typeDescriptor.upperBoundTypeDescriptor.toNonNullable() ==
        declarationTypeVariable.upperBoundTypeDescriptor.toNonNullable()
