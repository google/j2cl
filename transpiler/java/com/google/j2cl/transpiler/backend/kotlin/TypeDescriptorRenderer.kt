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
  // TODO(b/204287086): Move Kotlin -> Java type translation out of the renderer.
  val kotlinTypeName =
    when (typeDeclaration.qualifiedSourceName) {
      "java.lang.Annotation" -> "Annotation"
      "java.lang.Boolean" -> "Boolean"
      "java.lang.Byte" -> "Byte"
      "java.lang.Char" -> "Char"
      "java.lang.CharSequence" -> "CharSequence"
      "java.lang.Cloneable" -> "Cloneable"
      "java.lang.Comparable" -> "Comparable"
      "java.lang.Double" -> "Double"
      "java.lang.Enum" -> "Enum"
      "java.lang.Error" -> "Error"
      "java.lang.Exception" -> "Exception"
      "java.lang.Float" -> "Float"
      "java.lang.Integer" -> "Int"
      "java.lang.Iterable" -> "Iterable"
      "java.lang.Iterator" -> "Iterator"
      "java.lang.Long" -> "Long"
      "java.lang.Number" -> "Number"
      "java.lang.Object" -> "Any"
      "java.lang.Short" -> "Short"
      "java.lang.String" -> "String"
      "java.lang.Throwable" -> "Throwable"
      // TODO(b/202058120): Handle remaining types.
      else -> null
    }

  if (kotlinTypeName != null) {
    render(kotlinTypeName)
  } else {
    typeDeclaration.packageName?.let { packageName ->
      render("${packageName.packageNameSourceString}.")
    }
    render(typeDeclaration.simpleBinaryName.identifierSourceString)
  }
}

internal fun Renderer.render(typeDescriptor: TypeDescriptor) {
  render(typeDescriptor, isArgument = false)
}

internal fun Renderer.renderArgument(typeDescriptor: TypeDescriptor) {
  render(typeDescriptor, isArgument = true)
}

private fun Renderer.render(typeDescriptor: TypeDescriptor, isArgument: Boolean) {
  when (typeDescriptor) {
    is ArrayTypeDescriptor -> render(typeDescriptor)
    is DeclaredTypeDescriptor -> render(typeDescriptor)
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

private fun Renderer.render(declaredTypeDescriptor: DeclaredTypeDescriptor) {
  render(declaredTypeDescriptor.typeDeclaration)
  renderArguments(declaredTypeDescriptor)
  renderNullableSuffix(declaredTypeDescriptor)
}

private fun Renderer.renderArguments(declaredTypeDescriptor: DeclaredTypeDescriptor) {
  val arguments = declaredTypeDescriptor.typeArgumentDescriptors
  if (arguments.isNotEmpty()) {
    renderInAngleBrackets { renderCommaSeparated(arguments) { renderArgument(it) } }
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
    render(typeVariable.name.identifierSourceString)
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
