/*
 * Copyright 2021 Google Inc.
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

import com.google.j2cl.common.InternalCompilerError
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.ast.TypeVariable.createWildcard

/** Usage of type descriptor. */
enum class TypeDescriptorUsage {
  /** Type reference (RAW types are star-projected). */
  REFERENCE,

  // TODO(b/206611912): Remove when TypeVariable provides correct nullability.
  /** Generic argument (type variables are rendered without nullability). */
  ARGUMENT,

  /** Super-type declaration (RAW types are projected to bounds). */
  SUPER_TYPE,

  /** The type in instanceof expression. */
  INSTANCE_OF
}

internal fun Renderer.renderTypeDescriptor(
  typeDescriptor: TypeDescriptor,
  usage: TypeDescriptorUsage,
  seenTypeDescriptors: Set<DeclaredTypeDescriptor> = setOf(),
  asSimple: Boolean = false
) {
  when (typeDescriptor) {
    is ArrayTypeDescriptor -> renderArrayTypeDescriptor(typeDescriptor)
    is DeclaredTypeDescriptor ->
      renderDeclaredTypeDescriptor(typeDescriptor, usage, seenTypeDescriptors, asSimple = asSimple)
    is PrimitiveTypeDescriptor -> renderQualifiedName(typeDescriptor)
    is TypeVariable -> renderTypeVariable(typeDescriptor, usage)
    is IntersectionTypeDescriptor -> renderIntersectionTypeDescriptor(typeDescriptor)
    else -> throw InternalCompilerError("Unexpected ${typeDescriptor::class.java.simpleName}")
  }
}

private fun Renderer.renderNullableSuffix(typeDescriptor: TypeDescriptor) {
  if (typeDescriptor.isNullable) render("?")
}

private fun Renderer.renderArrayTypeDescriptor(arrayTypeDescriptor: ArrayTypeDescriptor) {
  renderQualifiedName(arrayTypeDescriptor)
  val componentTypeDescriptor = arrayTypeDescriptor.componentTypeDescriptor!!
  if (!componentTypeDescriptor.isPrimitive) {
    renderInAngleBrackets {
      renderTypeDescriptor(componentTypeDescriptor, usage = TypeDescriptorUsage.REFERENCE)
    }
  }
  renderNullableSuffix(arrayTypeDescriptor)
}

private fun Renderer.renderDeclaredTypeDescriptor(
  declaredTypeDescriptor: DeclaredTypeDescriptor,
  usage: TypeDescriptorUsage,
  seenTypeDescriptors: Set<DeclaredTypeDescriptor>,
  asSimple: Boolean
) {
  val typeDeclaration = declaredTypeDescriptor.typeDeclaration
  val enclosingTypeDescriptor = declaredTypeDescriptor.enclosingTypeDescriptor
  if (typeDeclaration.isLocal || asSimple) {
    // Skip rendering package name or enclosing type.
    renderIdentifier(typeDeclaration.ktSimpleName)
  } else if (enclosingTypeDescriptor != null) {
    // Render the enclosing type if present.
    if (!typeDeclaration.isCapturingEnclosingInstance) {
      renderQualifiedName(enclosingTypeDescriptor)
    } else {
      renderDeclaredTypeDescriptor(
        enclosingTypeDescriptor.toNonNullable(),
        usage,
        seenTypeDescriptors,
        asSimple = false
      )
    }
    render(".")
    renderIdentifier(typeDeclaration.ktSimpleName)
  } else {
    // Render the qualified or bridge name for this top-level type.
    var qualifiedName = typeDeclaration.ktQualifiedName
    if (usage == TypeDescriptorUsage.SUPER_TYPE && typeDeclaration.ktBridgeQualifiedName != null) {
      qualifiedName = typeDeclaration.ktBridgeQualifiedName
    }
    renderQualifiedName(qualifiedName)
  }

  renderTypeArguments(declaredTypeDescriptor, usage, seenTypeDescriptors)
  renderNullableSuffix(declaredTypeDescriptor)
}

private fun Renderer.renderTypeArguments(
  declaredTypeDescriptor: DeclaredTypeDescriptor,
  usage: TypeDescriptorUsage,
  seenTypeDescriptors: Set<DeclaredTypeDescriptor>
) {
  val seenTypeDescriptorsForArguments = seenTypeDescriptors + declaredTypeDescriptor
  val parameters = declaredTypeDescriptor.typeDeclaration.directlyDeclaredTypeParameterDescriptors
  val projectBounds =
    when (usage) {
      TypeDescriptorUsage.SUPER_TYPE,
      TypeDescriptorUsage.REFERENCE -> true
      TypeDescriptorUsage.INSTANCE_OF,
      TypeDescriptorUsage.ARGUMENT -> false
    }
  val arguments =
    typeArgumentDescriptors(declaredTypeDescriptor, projectBounds, seenTypeDescriptorsForArguments)
  if (arguments.isNotEmpty()) {
    if (usage != TypeDescriptorUsage.SUPER_TYPE || !arguments.any { it.isInferred }) {
      renderTypeArguments(parameters, arguments, seenTypeDescriptorsForArguments)
    }
  }
}

internal fun Renderer.renderTypeArguments(
  typeParameters: List<TypeVariable>,
  typeArguments: List<TypeDescriptor>,
  seenTypeDescriptors: Set<DeclaredTypeDescriptor> = setOf()
) {
  renderInAngleBrackets {
    renderCommaSeparated(inferNonNullableBounds(typeParameters, typeArguments)) {
      renderTypeDescriptor(it, TypeDescriptorUsage.ARGUMENT, seenTypeDescriptors)
    }
  }
}

private fun inferNonNullableBounds(
  typeParameters: List<TypeVariable>,
  typeArguments: List<TypeDescriptor>
): List<TypeDescriptor> = typeParameters.zip(typeArguments, ::inferNonNullableBounds)

private fun inferNonNullableBounds(
  typeParameter: TypeVariable,
  typeArgument: TypeDescriptor
): TypeDescriptor =
  if (!typeParameter.upperBoundTypeDescriptor.isNullable) typeArgument.toNonNullable()
  else typeArgument

private fun Renderer.renderTypeVariable(typeVariable: TypeVariable, usage: TypeDescriptorUsage) {
  if (typeVariable.isWildcardOrCapture) {
    val lowerBoundTypeDescriptor = typeVariable.lowerBoundTypeDescriptor
    if (lowerBoundTypeDescriptor != null) {
      render("in ")
      renderTypeDescriptor(lowerBoundTypeDescriptor, TypeDescriptorUsage.REFERENCE)
    } else {
      val boundTypeDescriptor = typeVariable.upperBoundTypeDescriptor
      if (isJavaLangObject(boundTypeDescriptor) && boundTypeDescriptor.isNullable) {
        render("*")
      } else {
        render("out ")
        renderTypeDescriptor(boundTypeDescriptor, TypeDescriptorUsage.REFERENCE)
      }
    }
  } else {
    renderName(typeVariable.toNullable())
    if (usage != TypeDescriptorUsage.ARGUMENT) renderNullableSuffix(typeVariable)
  }
}

private fun Renderer.renderIntersectionTypeDescriptor(
  intersectionTypeDescriptor: IntersectionTypeDescriptor
) {
  // Render only the first type from the intersection and comment out others, as they are not
  // supported in Kotlin.
  // TODO(b/205367162): Support intersection types.
  val typeDescriptors = intersectionTypeDescriptor.intersectionTypeDescriptors
  renderTypeDescriptor(typeDescriptors.first(), TypeDescriptorUsage.REFERENCE)
  render(" ")
  renderInCommentBrackets {
    render("& ")
    renderSeparatedWith(typeDescriptors.drop(1), " & ") {
      renderTypeDescriptor(it, TypeDescriptorUsage.REFERENCE)
    }
  }
}

/**
 * Returns type argument descriptors for rendering. RAW types will use projected arguments, using
 * wildcards or bounds if {@code projectBounds} flag is {@code true}.
 */
private fun Renderer.typeArgumentDescriptors(
  typeDescriptor: DeclaredTypeDescriptor,
  projectBounds: Boolean,
  seenTypeDescriptors: Set<DeclaredTypeDescriptor>
): List<TypeDescriptor> {
  val parameters = typeDescriptor.typeDeclaration.directlyDeclaredTypeParameterDescriptors
  val arguments = typeDescriptor.directlyDeclaredTypeArgumentDescriptors

  // Return original arguments for non-raw types.
  val isRaw = arguments.isEmpty() && parameters.isNotEmpty()
  if (!isRaw) return arguments

  // Convert type arguments to variables.
  val unparametrizedTypeDescriptor = typeDescriptor.toUnparameterizedTypeDescriptor()

  // Find variables which will be projected to bounds, others will be projected to wildcards.
  val boundProjectedVariables =
    if (projectBounds)
      unparametrizedTypeDescriptor.typeArgumentDescriptors.map { it as TypeVariable }.toSet()
    else setOf()

  // Replace variables with bounds or wildcards.
  val projectedTypeDescriptor =
    unparametrizedTypeDescriptor.specializeTypeVariables { variable ->
      val upperBound = variable.upperBoundTypeDescriptor.toRawTypeDescriptor()
      val isRecursive =
        upperBound is DeclaredTypeDescriptor && seenTypeDescriptors.contains(upperBound)
      val projectToBounds = boundProjectedVariables.contains(variable) && !isRecursive
      if (projectToBounds) upperBound else createWildcard()
    }

  return projectedTypeDescriptor.typeArgumentDescriptors
}

// TODO(b/216796920): Remove when the bug is fixed.
/** Type arguments declared directly on this type. */
private val DeclaredTypeDescriptor.directlyDeclaredTypeArgumentDescriptors: List<TypeDescriptor>
  get() = typeArgumentDescriptors.take(typeDeclaration.directlyDeclaredTypeParameterCount)

internal val TypeDescriptor.isInferred
  get() =
    (this is DeclaredTypeDescriptor && this.typeDeclaration.isAnonymous) ||
      (this is TypeVariable && this.isWildcardOrCapture)
