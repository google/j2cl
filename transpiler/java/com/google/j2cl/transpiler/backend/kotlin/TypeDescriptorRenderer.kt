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
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.ast.TypeVariable.createWildcard

/** Usage of type descriptor. */
enum class TypeDescriptorUsage {
  /** Type reference (RAW types are star-projected). */
  REFERENCE,

  /** Qualified name. */
  QUALIFIED_NAME,

  // TODO(b/206611912): Remove when TypeVariable provides correct nullability.
  /** Generic argument (type variables are rendered without nullability). */
  ARGUMENT,

  /** Super-type declaration (RAW types are projected to bounds). */
  SUPER_TYPE
}

internal fun Renderer.renderTypeDescriptor(
  typeDescriptor: TypeDescriptor,
  usage: TypeDescriptorUsage,
  asSimple: Boolean = false
) {
  when (typeDescriptor) {
    is ArrayTypeDescriptor -> renderArrayTypeDescriptor(typeDescriptor, usage)
    is DeclaredTypeDescriptor ->
      renderDeclaredTypeDescriptor(typeDescriptor, usage, asSimple = asSimple)
    is PrimitiveTypeDescriptor -> renderPrimitiveTypeDescriptor(typeDescriptor)
    is TypeVariable -> renderTypeVariable(typeDescriptor, usage)
    is IntersectionTypeDescriptor -> renderIntersectionTypeDescriptor(typeDescriptor)
    else -> throw InternalCompilerError("Unexpected ${typeDescriptor::class.java.simpleName}")
  }
}

private fun Renderer.renderNullableSuffix(typeDescriptor: TypeDescriptor) {
  if (typeDescriptor.isNullable) render("?")
}

private fun Renderer.renderArrayTypeDescriptor(
  arrayTypeDescriptor: ArrayTypeDescriptor,
  usage: TypeDescriptorUsage
) {
  when (val componentTypeDescriptor = arrayTypeDescriptor.componentTypeDescriptor!!) {
    PrimitiveTypes.BOOLEAN -> render("kotlin.BooleanArray")
    PrimitiveTypes.CHAR -> render("kotlin.CharArray")
    PrimitiveTypes.BYTE -> render("kotlin.ByteArray")
    PrimitiveTypes.SHORT -> render("kotlin.ShortArray")
    PrimitiveTypes.INT -> render("kotlin.IntArray")
    PrimitiveTypes.LONG -> render("kotlin.LongArray")
    PrimitiveTypes.FLOAT -> render("kotlin.FloatArray")
    PrimitiveTypes.DOUBLE -> render("kotlin.DoubleArray")
    else -> {
      render("kotlin.Array")
      if (usage != TypeDescriptorUsage.QUALIFIED_NAME) {
        renderInAngleBrackets {
          renderTypeDescriptor(componentTypeDescriptor, usage = TypeDescriptorUsage.REFERENCE)
        }
      }
    }
  }
  if (usage != TypeDescriptorUsage.QUALIFIED_NAME) {
    renderNullableSuffix(arrayTypeDescriptor)
  }
}

private fun Renderer.renderDeclaredTypeDescriptor(
  declaredTypeDescriptor: DeclaredTypeDescriptor,
  usage: TypeDescriptorUsage,
  asSimple: Boolean = false
) {
  val typeDeclaration = declaredTypeDescriptor.typeDeclaration
  val enclosingTypeDescriptor = declaredTypeDescriptor.enclosingTypeDescriptor
  if (typeDeclaration.isLocal || asSimple) {
    // Skip rendering package name or enclosing type.
    renderIdentifier(typeDeclaration.ktSimpleName)
  } else if (enclosingTypeDescriptor != null) {
    // Render the enclosing type if present.
    val enclosingUsage =
      if (!typeDeclaration.isCapturingEnclosingInstance) TypeDescriptorUsage.QUALIFIED_NAME
      else usage
    renderDeclaredTypeDescriptor(enclosingTypeDescriptor.toNonNullable(), enclosingUsage)
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

  if (usage != TypeDescriptorUsage.QUALIFIED_NAME) {
    renderArguments(declaredTypeDescriptor, usage)
    renderNullableSuffix(declaredTypeDescriptor)
  }
}

internal fun Renderer.renderArguments(
  declaredTypeDescriptor: DeclaredTypeDescriptor,
  usage: TypeDescriptorUsage
) {
  val parameters = declaredTypeDescriptor.typeDeclaration.renderedTypeParameterDescriptors
  val arguments =
    declaredTypeDescriptor.renderedTypeArgumentDescriptors(
      projectBounds = usage == TypeDescriptorUsage.SUPER_TYPE
    )
  if (arguments.isNotEmpty()) {
    if (usage != TypeDescriptorUsage.SUPER_TYPE || !arguments.any { it.isInferred }) {
      renderTypeArguments(parameters, arguments)
    }
  } else if (parameters.isNotEmpty()) {
    renderInAngleBrackets { renderCommaSeparated(parameters) { render("*") } }
  }
}

internal fun Renderer.renderTypeArguments(
  typeParameters: List<TypeVariable>,
  typeArguments: List<TypeDescriptor>
) {
  renderInAngleBrackets {
    renderCommaSeparated(inferNonNullableBounds(typeParameters, typeArguments)) {
      renderTypeDescriptor(it, TypeDescriptorUsage.ARGUMENT)
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

private fun Renderer.renderPrimitiveTypeDescriptor(
  primitiveTypeDescriptor: PrimitiveTypeDescriptor
) {
  render(
    when (primitiveTypeDescriptor) {
      PrimitiveTypes.VOID -> "kotlin.Unit"
      PrimitiveTypes.BOOLEAN -> "kotlin.Boolean"
      PrimitiveTypes.CHAR -> "kotlin.Char"
      PrimitiveTypes.BYTE -> "kotlin.Byte"
      PrimitiveTypes.SHORT -> "kotlin.Short"
      PrimitiveTypes.INT -> "kotlin.Int"
      PrimitiveTypes.LONG -> "kotlin.Long"
      PrimitiveTypes.FLOAT -> "kotlin.Float"
      PrimitiveTypes.DOUBLE -> "kotlin.Double"
      else -> throw InternalCompilerError("Unhandled $primitiveTypeDescriptor")
    }
  )
}

private fun Renderer.renderTypeVariable(typeVariable: TypeVariable, usage: TypeDescriptorUsage) {
  if (typeVariable.isWildcardOrCapture) {
    val lowerBoundTypeDescriptor = typeVariable.lowerBoundTypeDescriptor
    if (lowerBoundTypeDescriptor != null) {
      render("in ")
      renderTypeDescriptor(lowerBoundTypeDescriptor, TypeDescriptorUsage.REFERENCE)
    } else {
      val boundTypeDescriptor = typeVariable.upperBoundTypeDescriptor
      if (isJavaLangObject(boundTypeDescriptor)) {
        // TODO(b/202428351): Render upper type bounds if necessary.
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
private fun DeclaredTypeDescriptor.renderedTypeArgumentDescriptors(
  projectBounds: Boolean
): List<TypeDescriptor> {
  val parameters = typeDeclaration.renderedTypeParameterDescriptors
  val arguments = renderedTypeArgumentDescriptors

  // Return original arguments for non-raw types.
  val isRaw = arguments.isEmpty() && parameters.isNotEmpty()
  if (!isRaw) return arguments

  // Convert type arguments to variables.
  val typeDescriptor = toUnparameterizedTypeDescriptor()

  // Find variables which will be projected to bounds, others will be projected to wildcards.
  val variablesProjectedToBounds =
    if (projectBounds) typeDescriptor.typeArgumentDescriptors.map { it as TypeVariable }.toSet()
    else setOf()

  // Replace variables with bounds or wildcards.
  val projectedTypeDescriptor =
    typeDescriptor.specializeTypeVariables { variable ->
      if (variablesProjectedToBounds.contains(variable))
        variable.upperBoundTypeDescriptor.toRawTypeDescriptor()
      else createWildcard()
    }

  return projectedTypeDescriptor.typeArgumentDescriptors
}

// TODO(b/216796920): Remove when the bug is fixed.
private val DeclaredTypeDescriptor.renderedTypeArgumentDescriptors: List<TypeDescriptor>
  get() = typeArgumentDescriptors.take(typeDeclaration.renderedTypeParameterCount)

internal val TypeDescriptor.isInferred
  get() =
    (this is DeclaredTypeDescriptor && this.typeDeclaration.isAnonymous) ||
      (this is TypeVariable && this.isWildcardOrCapture)
