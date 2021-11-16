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

internal val TypeDescriptor.sourceString: String
  get() = sourceString(isArgument = false)

internal val TypeDescriptor.argumentSourceString: String
  get() = sourceString(isArgument = true)

// TODO(b/206611912): Remove the argument once TypeVariables can have different nullabilities.
private fun TypeDescriptor.sourceString(isArgument: Boolean): String =
  when (this) {
    is ArrayTypeDescriptor -> sourceString
    is DeclaredTypeDescriptor -> sourceString
    is PrimitiveTypeDescriptor -> sourceString
    is TypeVariable -> sourceString(isArgument)
    is IntersectionTypeDescriptor -> sourceString
    else -> throw InternalCompilerError("Unexpected ${this::class.java.simpleName}")
  }

private val TypeDescriptor.nullableSuffix
  get() = if (isNullable) "?" else ""

private val ArrayTypeDescriptor.sourceString: String
  get() =
    componentTypeDescriptor!!.let { typeDescriptor ->
      if (typeDescriptor is PrimitiveTypeDescriptor) {
        "${typeDescriptor.sourceString}Array$nullableSuffix"
      } else {
        "Array<${typeDescriptor.argumentSourceString}>$nullableSuffix"
      }
    }

private val DeclaredTypeDescriptor.sourceString: String
  get() = "${typeDeclaration.sourceString}$argumentsSourceString$nullableSuffix"

private val DeclaredTypeDescriptor.argumentsSourceString: String
  get() =
    typeArgumentDescriptors
      .takeIf { it.isNotEmpty() }
      ?.joinToString(", ") { it.argumentSourceString }
      ?.let { "<$it>" }
      ?: ""

private val PrimitiveTypeDescriptor.sourceString
  get() =
    when (this) {
      PrimitiveTypes.VOID -> "Unit"
      PrimitiveTypes.BOOLEAN -> "Boolean"
      PrimitiveTypes.CHAR -> "Char"
      PrimitiveTypes.BYTE -> "Byte"
      PrimitiveTypes.SHORT -> "Short"
      PrimitiveTypes.INT -> "Int"
      PrimitiveTypes.LONG -> "Long"
      PrimitiveTypes.FLOAT -> "Float"
      PrimitiveTypes.DOUBLE -> "Double"
      else -> throw InternalCompilerError("Unhandled $this")
    }

// TODO(b/203676284): Resolve unique name through Environment. Refactor all methods in this file
// to extension functions on Environment.
private fun TypeVariable.sourceString(isArgument: Boolean): String =
  if (isWildcardOrCapture) "*"
  else name.identifierSourceString.run { if (isArgument) this else plus(nullableSuffix) }

private val IntersectionTypeDescriptor.sourceString: String
  get() {
    // Render only the first type from the intersection and comment out others, as they are not
    // supported in Kotlin.
    // TODO(b/205367162): Support intersection types.
    val first = intersectionTypeDescriptors.first().sourceString
    val remaining = intersectionTypeDescriptors.drop(1).joinToString(" & ") { it.sourceString }
    return "$first /* & $remaining */"
  }

internal val TypeDeclaration.sourceString
  get() = mappedSourceStringOrNull ?: declaredSourceString

// TODO(b/204287086): Move out of renderer.
private val TypeDeclaration.mappedSourceStringOrNull
  get() =
    when (qualifiedSourceName) {
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

private val TypeDeclaration.declaredSourceString
  get() = "$packagePrefixSourceString${simpleBinaryName.identifierSourceString}"

private val TypeDeclaration.packagePrefixSourceString
  get() = packageName?.let { "${it.packageNameSourceString}." } ?: ""
