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

internal fun Renderer.renderTypeDescriptor(
  typeDescriptor: TypeDescriptor,
  // TODO(b/68726480): Currently, all TypeVariables are hardcoded as nullable.
  // Remove this parameter when TypeVariable models nullability correctly.
  skipTypeVariableNullability: Boolean = false,
  projectBounds: Boolean = false,
  asSimple: Boolean = false,
  asName: Boolean = false
) {
  when (typeDescriptor) {
    is ArrayTypeDescriptor -> renderArrayTypeDescriptor(typeDescriptor, asName = asName)
    is DeclaredTypeDescriptor ->
      renderDeclaredTypeDescriptor(
        typeDescriptor,
        asSimple = asSimple,
        asName = asName,
        projectBounds = projectBounds
      )
    is PrimitiveTypeDescriptor -> renderPrimitiveTypeDescriptor(typeDescriptor)
    is TypeVariable ->
      renderTypeVariable(typeDescriptor, skipNullability = skipTypeVariableNullability)
    is IntersectionTypeDescriptor -> renderIntersectionTypeDescriptor(typeDescriptor)
    else -> throw InternalCompilerError("Unexpected ${typeDescriptor::class.java.simpleName}")
  }
}

private fun Renderer.renderNullableSuffix(typeDescriptor: TypeDescriptor) {
  if (typeDescriptor.isNullable) render("?")
}

private fun Renderer.renderArrayTypeDescriptor(
  arrayTypeDescriptor: ArrayTypeDescriptor,
  asName: Boolean
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
      if (!asName) {
        renderInAngleBrackets { renderTypeDescriptor(componentTypeDescriptor) }
      }
    }
  }
  if (!asName) {
    renderNullableSuffix(arrayTypeDescriptor)
  }
}

private fun Renderer.renderDeclaredTypeDescriptor(
  declaredTypeDescriptor: DeclaredTypeDescriptor,
  asSimple: Boolean,
  asName: Boolean,
  projectBounds: Boolean
) {
  val typeDeclaration = declaredTypeDescriptor.typeDeclaration
  val enclosingTypeDescriptor = declaredTypeDescriptor.enclosingTypeDescriptor

  // Render the original Java type.
  if (asSimple || declaredTypeDescriptor.typeDeclaration.isLocal) {
    // Don't render package name or enclosing type for local types.
  } else if (enclosingTypeDescriptor != null) {
    // Render the enclosing type if present.
    renderDeclaredTypeDescriptor(
      enclosingTypeDescriptor.toNonNullable(),
      asSimple = asSimple,
      asName = asName || !typeDeclaration.isCapturingEnclosingInstance,
      projectBounds = projectBounds
    )
    render(".")
  } else {
    // Render the package name for this top-level type.
    val packageName = typeDeclaration.ktPackageName
    if (packageName != null) {
      renderPackageName(packageName)
      render(".")
    }
  }

  val name = typeDeclaration.ktSimpleName
  renderIdentifier(name)

  if (!asName) {
    renderArguments(declaredTypeDescriptor, projectBounds = projectBounds)
    renderNullableSuffix(declaredTypeDescriptor)
  }
}

internal fun Renderer.renderArguments(
  declaredTypeDescriptor: DeclaredTypeDescriptor,
  projectBounds: Boolean
) {
  val parameters = declaredTypeDescriptor.typeDeclaration.renderedTypeParameterDescriptors
  val arguments = declaredTypeDescriptor.renderedTypeArgumentDescriptors(projectBounds)
  if (arguments.isNotEmpty()) {
    renderInAngleBrackets {
      renderCommaSeparated(arguments) {
        renderTypeDescriptor(it, skipTypeVariableNullability = true)
      }
    }
  } else if (parameters.isNotEmpty()) {
    renderInAngleBrackets { renderCommaSeparated(parameters) { render("*") } }
  }
}

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

private fun Renderer.renderTypeVariable(typeVariable: TypeVariable, skipNullability: Boolean) {
  if (typeVariable.isWildcardOrCapture) {
    val lowerBoundTypeDescriptor = typeVariable.lowerBoundTypeDescriptor
    if (lowerBoundTypeDescriptor != null) {
      render("in ")
      renderTypeDescriptor(lowerBoundTypeDescriptor)
    } else {
      val boundTypeDescriptor = typeVariable.upperBoundTypeDescriptor
      if (isJavaLangObject(boundTypeDescriptor)) {
        // TODO(b/202428351): Render upper type bounds if necessary.
        render("*")
      } else {
        render("out ")
        renderTypeDescriptor(boundTypeDescriptor)
      }
    }
  } else {
    renderName(typeVariable)
    if (!skipNullability) renderNullableSuffix(typeVariable)
  }
}

private fun Renderer.renderIntersectionTypeDescriptor(
  intersectionTypeDescriptor: IntersectionTypeDescriptor
) {
  // Render only the first type from the intersection and comment out others, as they are not
  // supported in Kotlin.
  // TODO(b/205367162): Support intersection types.
  val typeDescriptors = intersectionTypeDescriptor.intersectionTypeDescriptors
  renderTypeDescriptor(typeDescriptors.first())
  render(" ")
  renderInCommentBrackets {
    render("& ")
    renderSeparatedWith(typeDescriptors.drop(1), " & ") { renderTypeDescriptor(it) }
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
