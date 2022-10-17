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
import com.google.j2cl.transpiler.ast.MemberDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.Visibility

internal val MemberDescriptor.ktMangledName: String
  get() =
    ktName +
      when (visibility) {
        Visibility.PUBLIC -> ""
        Visibility.PROTECTED -> ""
        Visibility.PACKAGE_PRIVATE -> ""
        Visibility.PRIVATE -> "_private_${enclosingTypeDescriptor.typeDeclaration.mangledName}"
      }

internal fun TypeDeclaration.ktSimpleName(asSuperType: Boolean = false) =
  if (asSuperType) ktBridgeSimpleName ?: ktSimpleName else ktSimpleName

internal fun TypeDeclaration.ktQualifiedName(asSuperType: Boolean = false) =
  if (asSuperType) ktBridgeQualifiedName ?: ktQualifiedName else ktQualifiedName

internal fun TypeDescriptor.ktQualifiedName(asSuperType: Boolean = false): String =
  when (this) {
    is PrimitiveTypeDescriptor ->
      when (this) {
        PrimitiveTypes.VOID -> "kotlin.Unit"
        PrimitiveTypes.BOOLEAN -> "kotlin.Boolean"
        PrimitiveTypes.CHAR -> "kotlin.Char"
        PrimitiveTypes.BYTE -> "kotlin.Byte"
        PrimitiveTypes.SHORT -> "kotlin.Short"
        PrimitiveTypes.INT -> "kotlin.Int"
        PrimitiveTypes.LONG -> "kotlin.Long"
        PrimitiveTypes.FLOAT -> "kotlin.Float"
        PrimitiveTypes.DOUBLE -> "kotlin.Double"
        else -> null
      }
    is ArrayTypeDescriptor ->
      when (componentTypeDescriptor!!) {
        PrimitiveTypes.BOOLEAN -> "kotlin.BooleanArray"
        PrimitiveTypes.CHAR -> "kotlin.CharArray"
        PrimitiveTypes.BYTE -> "kotlin.ByteArray"
        PrimitiveTypes.SHORT -> "kotlin.ShortArray"
        PrimitiveTypes.INT -> "kotlin.IntArray"
        PrimitiveTypes.LONG -> "kotlin.LongArray"
        PrimitiveTypes.FLOAT -> "kotlin.FloatArray"
        PrimitiveTypes.DOUBLE -> "kotlin.DoubleArray"
        else -> "kotlin.Array"
      }
    is DeclaredTypeDescriptor -> typeDeclaration.ktQualifiedName(asSuperType)
    else -> null
  }
    ?: error("$this.ktQualifiedName(asSuperType = $asSuperType)")
