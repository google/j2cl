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

import com.google.j2cl.common.InternalCompilerError
import com.google.j2cl.common.StringUtils
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.Field
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.ast.Variable
import com.google.j2cl.transpiler.ast.Visibility
import com.google.j2cl.transpiler.backend.kotlin.common.letIf
import com.google.j2cl.transpiler.backend.kotlin.common.mapFirst

internal fun Renderer.renderOptInExperimentalObjCNameFileAnnotation() {
  render("@file:")
  renderTopLevelQualifiedName("kotlin.OptIn")
  renderInParentheses {
    renderTopLevelQualifiedName("kotlin.experimental.ExperimentalObjCName")
    render("::class")
  }
}

internal fun Renderer.renderObjCNameAnnotation(name: String, exact: Boolean? = null) {
  render("@")
  renderTopLevelQualifiedName("kotlin.native.ObjCName")
  renderInParentheses {
    renderString(name)
    exact?.let { render(", exact = $it") }
  }
}

internal val Field.objCName: String?
  get() =
    if (descriptor.visibility.needsObjCNameAnnotation) {
      val objCPrefix = descriptor.enclosingTypeDescriptor.typeDeclaration.objCName(forMember = true)
      val name = descriptor.ktMangledName
      if (descriptor.isStatic) "${objCPrefix}_${name.identifierString}" else "${name}_"
    } else null

internal data class MethodObjCNames(
  val methodName: String? = null,
  val parameterNames: List<String>
)

internal fun Method.toObjCNames(): MethodObjCNames? =
  if (!descriptor.needsObjCNameAnnotations) null
  else if (descriptor.isConstructor) toConstructorObjCNames() else toNonConstructorObjCNames()

private fun Method.toConstructorObjCNames(): MethodObjCNames =
  descriptor.objectiveCName.let { objectiveCName ->
    MethodObjCNames(
      objectiveCName,
      if (objectiveCName != null) {
        objectiveCName.split(":").mapFirst {
          val prefix = "initWith"
          if (it.startsWith(prefix)) it.substring(prefix.length) else parameters.first().objCName
        }
      } else {
        parameters.mapIndexed { index, parameter ->
          parameter.objCName.letIf(index != 0) { "with$it" }
        }
      }
    )
  }

private fun Method.toNonConstructorObjCNames(): MethodObjCNames =
  descriptor.objectiveCName.let { objectiveCName ->
    if (objectiveCName == null || parameters.isEmpty()) {
      MethodObjCNames(objectiveCName, parameters.map { "with${it.objCName}" })
    } else {
      val methodName = descriptor.name!!
      val prefix = methodName.commonPrefixWith(objectiveCName)
      val objCParameterNames = objectiveCName.split(":")
      val firstObjCParameterName = objCParameterNames.firstOrNull()
      if (firstObjCParameterName == null) {
        MethodObjCNames(objectiveCName, objCParameterNames)
      } else {
        // If possible, split method name and first parameter by shared prefix.
        // Otherwise, split string in half arbitrarily. Does not handle single character objc name.
        if (prefix.isNotEmpty() && prefix.length != firstObjCParameterName.length) {
          MethodObjCNames(prefix, objCParameterNames.mapFirst { it.substring(prefix.length) })
        } else {
          check(firstObjCParameterName.length > 1)
          val midIndex = firstObjCParameterName.length / 2
          MethodObjCNames(
            firstObjCParameterName.substring(0, midIndex),
            objCParameterNames.mapFirst { it.substring(midIndex) }
          )
        }
      }
    }
  }

internal val TypeDeclaration.objCName: String
  get() = objCName(forMember = false)

internal fun TypeDeclaration.objCName(forMember: Boolean): String =
  objectiveCName ?: mappedObjCName ?: defaultObjCName(forMember = forMember)

private val TypeDeclaration.mappedObjCName: String?
  get() =
    when (qualifiedBinaryName) {
      "java.lang.Object" -> "NSObject"
      "java.lang.String" -> "NSString"
      "java.lang.Class" -> "IOSClass"
      "java.lang.Number" -> "NSNumber"
      "java.lang.Cloneable" -> "NSCopying"
      else -> null
    }

private fun TypeDeclaration.defaultObjCName(forMember: Boolean): String =
  simpleObjCName.let { simpleObjCName ->
    enclosingTypeDeclaration?.let { it.objCName(forMember = forMember) + "_" + simpleObjCName }
      ?: objectiveCNamePrefix?.let { it + simpleObjCName }
        ?: (objCPackagePrefix(forMember = forMember) + simpleObjCName)
  }

private val TypeDeclaration.simpleObjCName: String
  get() = simpleSourceName.objCName

private fun TypeDeclaration.objCPackagePrefix(forMember: Boolean): String =
  packageName?.objCPackagePrefix(forMember = forMember) ?: ""

private fun String.objCPackagePrefix(forMember: Boolean): String =
  this
    // TODO(b/265295531): This line is a temporary hack, remove when not needed.
    .letIf(!forMember && (startsWith("java.") || startsWith("javax."))) { "j2kt.$it" }
    .split('.')
    .joinToString(separator = "") { it.titleCase.objCName }

private val String.titleCase
  get() = StringUtils.capitalize(this)

private val String.objCName
  get() = replace('$', '_')

private fun TypeDescriptor.objCName(useId: Boolean, forMember: Boolean): String =
  when (this) {
    is PrimitiveTypeDescriptor -> {
      when (this) {
        PrimitiveTypes.BOOLEAN -> "boolean"
        PrimitiveTypes.BYTE -> "byte"
        PrimitiveTypes.SHORT -> "short"
        PrimitiveTypes.INT -> "int"
        PrimitiveTypes.LONG -> "long"
        PrimitiveTypes.CHAR -> "char"
        PrimitiveTypes.FLOAT -> "float"
        PrimitiveTypes.DOUBLE -> "double"
        // TODO(b/259416922): figure out how to handle Void or void
        else -> throw InternalCompilerError("Unexpected ${this::class.java.simpleName}")
      }
    }
    is ArrayTypeDescriptor ->
      leafTypeDescriptor.objCName(useId = false, forMember = forMember) + "Array" + dimensionsSuffix
    is DeclaredTypeDescriptor ->
      if (useId && isJavaLangObject(this)) "id" else typeDeclaration.objCName(forMember = forMember)
    is TypeVariable -> upperBoundTypeDescriptor.objCName(useId = useId, forMember = forMember)
    else -> "id"
  }

private val ArrayTypeDescriptor.dimensionsSuffix
  get() = if (dimensions > 1) "$dimensions" else ""

private val Variable.objCName
  get() = typeDescriptor.objCName(useId = true, forMember = true).titleCase

private val MethodDescriptor.needsObjCNameAnnotations
  get() = visibility.needsObjCNameAnnotation && !isKtOverride

internal val Visibility.needsObjCNameAnnotation
  get() = this == Visibility.PUBLIC || this == Visibility.PROTECTED
