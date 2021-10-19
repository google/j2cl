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
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeVariable

internal val TypeDescriptor.sourceString: String
  get() =
    when (this) {
      is ArrayTypeDescriptor -> typeDescriptorSourceString
      is DeclaredTypeDescriptor -> typeDescriptorSourceString
      is PrimitiveTypeDescriptor -> typeDescriptorSourceString
      is TypeVariable -> typeDescriptorSourceString
      else -> throw InternalCompilerError("Unhandled $this")
    }

private val ArrayTypeDescriptor.typeDescriptorSourceString
  get() =
    componentTypeDescriptor!!.let { typeDescriptor ->
      if (typeDescriptor is PrimitiveTypeDescriptor) {
        "${typeDescriptor.sourceString}Array$nullableSuffix"
      } else {
        "Array<${typeDescriptor.sourceString}>$nullableSuffix"
      }
    }

private val TypeDescriptor.nullableSuffix
  get() = if (isNullable) "?" else ""

private val DeclaredTypeDescriptor.typeDescriptorSourceString
  get() = "${typeDeclaration.sourceString}$argumentsSourceString$nullableSuffix"

private val DeclaredTypeDescriptor.argumentsSourceString
  get() =
    typeArgumentDescriptors
      .takeIf { it.isNotEmpty() }
      ?.joinToString(", ") { it.sourceString }
      ?.let { "<$it>" }
      ?: ""

private val PrimitiveTypeDescriptor.typeDescriptorSourceString
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

private val TypeVariable.typeDescriptorSourceString
  get() = if (isWildcardOrCapture) "*" else name

private val TypeDeclaration.sourceString
  get() = mappedSourceStringOrNull ?: declaredSourceString

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

internal val TypeDeclaration.declaredSourceString
  get() = "$packagePrefixSourceString$classComponentsSourceString"

private val TypeDeclaration.packagePrefixSourceString
  get() = packageName?.let { "${it.packageNameSourceString}." } ?: ""

private val TypeDeclaration.classComponentsSourceString
  get() = classComponents.joinToString(".") { it.identifierSourceString }
