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
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.ast.TypeVariable.createWildcardWithBound

internal fun Renderer.mapsToKotlin(typeDescriptor: DeclaredTypeDescriptor) =
  typeDescriptor.typeDeclaration.qualifiedBinaryName.mappedKotlinQualifiedName != null

// TODO(b/216924456): Remove when KtNative annotations are present in the sources.
private val String.mappedKotlinQualifiedName: String?
  get() =
    when (this) {
      "java.lang.Annotation" -> "kotlin.Annotation"
      "java.lang.Boolean" -> "kotlin.Boolean"
      "java.lang.Byte" -> "kotlin.Byte"
      "java.lang.Character" -> "kotlin.Char"
      "java.lang.CharSequence" -> "kotlin.CharSequence"
      "java.lang.Cloneable" -> "kotlin.Cloneable"
      "java.lang.Comparable" -> "kotlin.Comparable"
      "java.lang.Double" -> "kotlin.Double"
      "java.lang.Enum" -> "kotlin.Enum"
      "java.lang.Float" -> "kotlin.Float"
      "java.lang.Integer" -> "kotlin.Int"
      "java.lang.Iterable" -> "kotlin.collections.MutableIterable"
      "java.lang.Long" -> "kotlin.Long"
      "java.lang.Number" -> "kotlin.Number"
      "java.lang.Object" -> "kotlin.Any"
      "java.lang.Short" -> "kotlin.Short"
      "java.lang.String" -> "kotlin.String"
      "java.lang.Throwable" -> "kotlin.Throwable"
      "java.util.Collection" -> "kotlin.collections.MutableCollection"
      "java.util.Iterator" -> "kotlin.collections.MutableIterator"
      "java.util.List" -> "kotlin.collections.MutableList"
      "java.util.ListIterator" -> "kotlin.collections.MutableListIterator"
      "java.util.Map" -> "kotlin.collections.MutableMap"
      "java.util.Map.Entry" -> "kotlin.collections.MutableMap.MutableEntry"
      "java.util.Set" -> "kotlin.collections.MutableSet"
      else -> null
    }

internal fun Renderer.renderTypeDescriptor(
  typeDescriptor: TypeDescriptor,
  isArgument: Boolean = false,
  asJava: Boolean = false,
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
        asJava = asJava,
        asName = asName,
        projectBounds = projectBounds
      )
    is PrimitiveTypeDescriptor -> renderPrimitiveTypeDescriptor(typeDescriptor)
    is TypeVariable -> renderTypeVariable(typeDescriptor, isArgument = isArgument)
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
    PrimitiveTypes.BOOLEAN -> render("BooleanArray")
    PrimitiveTypes.CHAR -> render("CharArray")
    PrimitiveTypes.BYTE -> render("ByteArray")
    PrimitiveTypes.SHORT -> render("ShortArray")
    PrimitiveTypes.INT -> render("IntArray")
    PrimitiveTypes.LONG -> render("LongArray")
    PrimitiveTypes.FLOAT -> render("FloatArray")
    PrimitiveTypes.DOUBLE -> render("DoubleArray")
    else -> {
      render("Array")
      if (!asName) {
        renderInAngleBrackets { renderTypeDescriptor(componentTypeDescriptor, isArgument = true) }
      }
    }
  }
  if (!asName) {
    renderNullableSuffix(arrayTypeDescriptor)
  }
}

private fun Renderer.renderDeclaredTypeDescriptor(
  declaredTypeDescriptor: DeclaredTypeDescriptor,
  asJava: Boolean,
  asSimple: Boolean,
  asName: Boolean,
  projectBounds: Boolean
) {
  // Check if the Java type maps to Kotlin one, ie: java.lang.String -> kotlin.String
  val mappedKotlinName =
    declaredTypeDescriptor.qualifiedSourceName.takeUnless { asJava }?.run {
      mappedKotlinQualifiedName
    }

  if (mappedKotlinName != null) {
    // Render the mapped Kotlin type name.
    renderQualifiedName(mappedKotlinName)
  } else {
    val typeDeclaration = declaredTypeDescriptor.typeDeclaration
    val enclosingTypeDescriptor = declaredTypeDescriptor.enclosingTypeDescriptor

    // Render the original Java type.
    if (asSimple || declaredTypeDescriptor.typeDeclaration.isLocal) {
      // Don't render package name or enclosing type for local types.
    } else if (enclosingTypeDescriptor != null) {
      // Render the enclosing type if present.
      renderDeclaredTypeDescriptor(
        enclosingTypeDescriptor.toNonNullable(),
        asJava = asJava,
        asSimple = asSimple,
        asName = asName || !typeDeclaration.isCapturingEnclosingInstance,
        projectBounds = projectBounds
      )
      render(".")
    } else {
      // Render the package name for this top-level type.
      val packageName = if (asJava) typeDeclaration.packageName else typeDeclaration.ktPackageName
      if (packageName != null) {
        renderPackageName(packageName)
        render(".")
      }
    }

    val name = if (asJava) typeDeclaration.simpleSourceName else typeDeclaration.ktSimpleName
    renderIdentifier(name)
  }

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
      renderCommaSeparated(arguments) { renderTypeDescriptor(it, isArgument = true) }
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
      PrimitiveTypes.VOID -> "Unit"
      PrimitiveTypes.BOOLEAN -> "Boolean"
      PrimitiveTypes.CHAR -> "Char"
      PrimitiveTypes.BYTE -> "Byte"
      PrimitiveTypes.SHORT -> "Short"
      PrimitiveTypes.INT -> "Int"
      PrimitiveTypes.LONG -> "Long"
      PrimitiveTypes.FLOAT -> "Float"
      PrimitiveTypes.DOUBLE -> "Double"
      else -> throw InternalCompilerError("Unhandled $primitiveTypeDescriptor")
    }
  )
}

private fun Renderer.renderTypeVariable(typeVariable: TypeVariable, isArgument: Boolean) {
  if (typeVariable.isWildcardOrCapture) {
    render("*")
  } else {
    renderName(typeVariable)
    if (!isArgument) renderNullableSuffix(typeVariable)
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
        variable.boundTypeDescriptor.toRawTypeDescriptor()
      else createWildcardWithBound(variable.boundTypeDescriptor)
    }

  return projectedTypeDescriptor.typeArgumentDescriptors
}

// TODO(b/216796920): Remove when the bug is fixed.
private val DeclaredTypeDescriptor.renderedTypeArgumentDescriptors: List<TypeDescriptor>
  get() {
    val enclosingTypeDescriptor = enclosingTypeDescriptor
    return if (enclosingTypeDescriptor == null || !typeDeclaration.isCapturingEnclosingInstance)
      typeArgumentDescriptors
    else typeArgumentDescriptors.dropLast(enclosingTypeDescriptor.typeArgumentDescriptors.size)
  }
