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
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeVariable

internal fun Renderer.render(typeDeclaration: TypeDeclaration) {
  render(typeDeclaration, asJavaType = false)
}

internal fun Renderer.renderAsJavaType(typeDeclaration: TypeDeclaration) {
  render(typeDeclaration, asJavaType = true)
}

private fun Renderer.render(typeDeclaration: TypeDeclaration, asJavaType: Boolean) {
  val javaQualifiedName = typeDeclaration.qualifiedBinaryName
  val mappedKotlinQualifiedName =
    javaQualifiedName.takeUnless { asJavaType }?.run { mappedKotlinQualifiedName }
  renderQualifiedName(mappedKotlinQualifiedName ?: javaQualifiedName)
}

internal fun Renderer.mapsToKotlin(typeDescriptor: DeclaredTypeDescriptor) =
  typeDescriptor.typeDeclaration.qualifiedBinaryName.mappedKotlinQualifiedName != null

private val String.mappedKotlinQualifiedName: String?
  get() =
    // TODO(b/204287086): Move Kotlin -> Java type translation out of the renderer.
    when (this) {
      "java.lang.Boolean" -> "kotlin.Boolean"
      "java.lang.Byte" -> "kotlin.Byte"
      "java.lang.Cloneable" -> "kotlin.Cloneable"
      "java.lang.Char" -> "kotlin.Char"
      "java.lang.CharSequence" -> "kotlin.CharSequence"
      "java.lang.Double" -> "kotlin.Double"
      "java.lang.Enum" -> "kotlin.Enum"
      "java.lang.Float" -> "kotlin.Float"
      "java.lang.Integer" -> "kotlin.Int"
      "java.lang.Long" -> "kotlin.Long"
      "java.lang.Number" -> "kotlin.Number"
      "java.lang.Object" -> "kotlin.Any"
      "java.lang.Short" -> "kotlin.Short"
      "java.lang.String" -> "kotlin.String"
      "java.lang.Throwable" -> "kotlin.Throwable"
      // TODO(b/202058120): Handle remaining types.
      else -> null
    }

internal fun Renderer.render(typeDescriptor: TypeDescriptor) {
  render(typeDescriptor, isArgument = false, asJavaType = false)
}

internal fun Renderer.renderArgument(typeDescriptor: TypeDescriptor) {
  render(typeDescriptor, isArgument = true, asJavaType = false)
}

internal fun Renderer.renderAsJavaType(typeDescriptor: TypeDescriptor) {
  render(typeDescriptor, isArgument = false, asJavaType = true)
}

private fun Renderer.render(
  typeDescriptor: TypeDescriptor,
  isArgument: Boolean = false,
  asJavaType: Boolean = false
) {
  when (typeDescriptor) {
    is ArrayTypeDescriptor -> render(typeDescriptor)
    is DeclaredTypeDescriptor -> render(typeDescriptor, asJavaType)
    is PrimitiveTypeDescriptor -> render(typeDescriptor)
    is TypeVariable -> render(typeDescriptor, isArgument)
    is IntersectionTypeDescriptor -> render(typeDescriptor)
    else -> throw InternalCompilerError("Unexpected ${typeDescriptor::class.java.simpleName}")
  }
}

private fun Renderer.renderNullableSuffix(typeDescriptor: TypeDescriptor) {
  if (typeDescriptor.isNullable) render("?")
}

private fun Renderer.render(arrayTypeDescriptor: ArrayTypeDescriptor) {
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
      renderInAngleBrackets { renderArgument(componentTypeDescriptor) }
    }
  }
  renderNullableSuffix(arrayTypeDescriptor)
}

private fun Renderer.render(declaredTypeDescriptor: DeclaredTypeDescriptor, asJavaType: Boolean) {
  render(declaredTypeDescriptor.typeDeclaration, asJavaType)
  renderArguments(declaredTypeDescriptor)
  renderNullableSuffix(declaredTypeDescriptor)
}

private fun Renderer.renderArguments(declaredTypeDescriptor: DeclaredTypeDescriptor) {
  val parameters = declaredTypeDescriptor.typeDeclaration.typeParameterDescriptors
  val arguments = declaredTypeDescriptor.typeArgumentDescriptors
  if (arguments.isNotEmpty()) {
    renderInAngleBrackets { renderCommaSeparated(arguments) { renderArgument(it) } }
  } else if (parameters.isNotEmpty()) {
    renderInAngleBrackets { renderCommaSeparated(parameters) { render("*") } }
  }
}

private fun Renderer.render(primitiveTypeDescriptor: PrimitiveTypeDescriptor) {
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

private fun Renderer.render(typeVariable: TypeVariable, isArgument: Boolean) {
  if (typeVariable.isWildcardOrCapture) {
    render("*")
  } else {
    renderName(typeVariable)
    if (!isArgument) renderNullableSuffix(typeVariable)
  }
}

private fun Renderer.render(intersectionTypeDescriptor: IntersectionTypeDescriptor) {
  // Render only the first type from the intersection and comment out others, as they are not
  // supported in Kotlin.
  // TODO(b/205367162): Support intersection types.
  val typeDescriptors = intersectionTypeDescriptor.intersectionTypeDescriptors
  render(typeDescriptors.first())
  render(" ")
  renderInCommentBrackets {
    render("& ")
    renderSeparatedWith(typeDescriptors.drop(1), " & ") { render(it) }
  }
}
