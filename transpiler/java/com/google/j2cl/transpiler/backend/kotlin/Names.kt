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
import com.google.j2cl.transpiler.ast.CompilationUnit
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.Field
import com.google.j2cl.transpiler.ast.FieldDescriptor
import com.google.j2cl.transpiler.ast.MemberDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.Visibility
import com.google.j2cl.transpiler.backend.kotlin.ast.Member
import com.google.j2cl.transpiler.backend.kotlin.ast.kotlinMembers

internal val Type.localNames: Set<String>
  get() =
    kotlinMembers
      .map { member ->
        when (member) {
          is Member.WithCompanionObject -> null
          is Member.WithJavaMember -> (member.javaMember as? Field)?.descriptor?.ktName
          is Member.WithType -> member.type.declaration.ktSimpleName
        }
      }
      .filterNotNull()
      .toSet()

internal val CompilationUnit.topLevelQualifiedNames: Set<String>
  get() = types.map { it.declaration }.filter { !it.isKtNative }.map { it.ktQualifiedName }.toSet()

internal val MemberDescriptor.ktMangledName: String
  get() = ktName + ktNameSuffix

private val MemberDescriptor.ktNameSuffix: String
  get() =
    when (visibility!!) {
      Visibility.PUBLIC -> ktPropertyNameSuffix
      Visibility.PROTECTED -> ktPropertyNameSuffix
      Visibility.PACKAGE_PRIVATE ->
        "_pp_${enclosingTypeDescriptor.typeDeclaration.packageName?.replace('.', '_') ?: ""}"
      Visibility.PRIVATE ->
        "_private_${enclosingTypeDescriptor.typeDeclaration.privateMemberSuffix}"
    }

private val MemberDescriptor.ktPropertyNameSuffix: String
  get() = if (this is FieldDescriptor && hasConflictingKtProperty) "_ktPropertyConflict" else ""

private val FieldDescriptor.hasConflictingKtProperty: Boolean
  get() = enclosingTypeDescriptor.polymorphicMethods.any { it.isKtProperty && it.ktName == ktName }

private val TypeDeclaration.privateMemberSuffix: String
  get() = if (isInterface) mangledName else "$classHierarchyDepth"

internal fun TypeDeclaration.ktSimpleName(asSuperType: Boolean = false) =
  if (asSuperType) ktBridgeSimpleName ?: ktSimpleName else ktSimpleName

internal fun TypeDeclaration.ktQualifiedName(asSuperType: Boolean = false) =
  if (asSuperType) ktBridgeQualifiedName ?: ktQualifiedName else ktQualifiedName

internal fun TypeDescriptor.ktQualifiedName(asSuperType: Boolean = false): String =
  when (this) {
    is PrimitiveTypeDescriptor -> toBoxedType().ktQualifiedName(asSuperType)
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

internal fun String.qualifiedNameComponents(): List<String> = split(".")

internal fun String.qualifiedNameToSimpleName(): String = qualifiedNameComponents().last()

internal fun String.qualifiedNameToAlias(): String = qualifiedNameComponents().joinToString("_")
