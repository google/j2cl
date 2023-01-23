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
import com.google.j2cl.transpiler.ast.FieldDescriptor
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
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.ifNotNullSource
import com.google.j2cl.transpiler.backend.kotlin.source.inRoundBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.join
import com.google.j2cl.transpiler.backend.kotlin.source.source
import com.google.j2cl.transpiler.backend.kotlin.source.sourceIf

internal fun Renderer.optInExperimentalObjCNameFileAnnotationSource(): Source =
  join(
    source("@file:"),
    topLevelQualifiedNameSource("kotlin.OptIn"),
    inRoundBrackets(
      join(
        topLevelQualifiedNameSource("kotlin.experimental.ExperimentalObjCName"),
        source("::class")
      )
    )
  )

internal fun Renderer.objCNameAnnotationSource(name: String, exact: Boolean? = null): Source =
  join(
    at(topLevelQualifiedNameSource("kotlin.native.ObjCName")),
    inRoundBrackets(
      commaSeparated(
        literalSource(name),
        exact.ifNotNullSource { parameterSource("exact", literalSource(it)) }
      )
    )
  )

internal fun Renderer.objCNameAnnotationSource(typeDeclaration: TypeDeclaration): Source =
  sourceIf(typeDeclaration.needsObjCNameAnnotation) {
    objCNameAnnotationSource(typeDeclaration.objCName, exact = true)
  }

internal fun Renderer.objCNameAnnotationSource(
  methodDescriptor: MethodDescriptor,
  methodObjCNames: MethodObjCNames?
): Source =
  sourceIf(!methodDescriptor.isConstructor) {
    methodObjCNames?.methodName.ifNotNullSource { objCNameAnnotationSource(it, exact = false) }
  }

internal fun Renderer.objCNameAnnotationSource(fieldDescriptor: FieldDescriptor): Source =
  sourceIf(fieldDescriptor.needsObjCNameAnnotations) {
    objCNameAnnotationSource(fieldDescriptor.objCName, exact = false)
  }

private fun parameterSource(name: String, valueSource: Source): Source =
  assignment(source(name), valueSource)

internal data class MethodObjCNames(
  val methodName: String? = null,
  val parameterNames: List<String>
)

internal fun Method.toObjCNames(): MethodObjCNames? =
  if (!descriptor.needsObjCNameAnnotations) null
  else if (descriptor.isConstructor) toConstructorObjCNames() else toNonConstructorObjCNames()

private val MethodDescriptor.needsObjCNameAnnotations
  get() = visibility.needsObjCNameAnnotation && !isKtOverride

private val FieldDescriptor.needsObjCNameAnnotations
  get() =
    enclosingTypeDescriptor.typeDeclaration.needsObjCNameAnnotation &&
      visibility.needsObjCNameAnnotation &&
      isStatic

private val TypeDeclaration.needsObjCNameAnnotation
  get() = visibility.needsObjCNameAnnotation && !isLocal

private val Visibility.needsObjCNameAnnotation
  get() = this == Visibility.PUBLIC || this == Visibility.PROTECTED

private fun Method.toConstructorObjCNames(): MethodObjCNames =
  descriptor.objectiveCName.let { objectiveCName ->
    MethodObjCNames(
      objectiveCName,
      if (objectiveCName != null) {
        objectiveCName.objCMethodParameterNames.mapFirst {
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
      val objCParameterNames = objectiveCName.objCMethodParameterNames
      val firstObjCParameterName = objCParameterNames.firstOrNull()
      if (firstObjCParameterName == null) {
        MethodObjCNames(objectiveCName, objCParameterNames)
      } else {
        // Split string by index of last uppercase or in half arbitrarily. Does not
        // handle single character objc name.
        check(firstObjCParameterName.length > 1)
        val splitIndex =
          firstObjCParameterName.indexOfLast { it.isUpperCase() }.takeIf { it > 0 }
            ?: (firstObjCParameterName.length / 2)
        MethodObjCNames(
          firstObjCParameterName.substring(0, splitIndex),
          objCParameterNames.mapFirst { it.substring(splitIndex) }
        )
      }
    }
  }

private val String.objCMethodParameterNames: List<String>
  get() = letIf(lastOrNull() == ':') { dropLast(1) }.split(":")

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

internal fun TypeDeclaration.defaultObjCName(forMember: Boolean): String =
  objCNamePrefix(forMember) + simpleObjCName

private fun TypeDeclaration.objCNamePrefix(forMember: Boolean) =
  enclosingTypeDeclaration.run {
    if (this != null) objCName(forMember = forMember) + "_"
    else simpleObjCNamePrefix(forMember = forMember)
  }

private fun TypeDeclaration.simpleObjCNamePrefix(forMember: Boolean) =
  objectiveCNamePrefix ?: objCPackagePrefix(forMember = forMember)

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

internal val String.titleCase
  get() = StringUtils.capitalize(this)

internal val String.objCName
  get() = replace('$', '_')

private const val idObjCName = "id"

internal fun TypeDescriptor.objCName(useId: Boolean, forMember: Boolean): String =
  when (this) {
    is PrimitiveTypeDescriptor -> primitiveObjCName
    is ArrayTypeDescriptor -> arrayObjCName(forMember = forMember)
    is DeclaredTypeDescriptor -> declaredObjCName(useId = useId, forMember = forMember)
    is TypeVariable -> variableObjCName(useId = useId, forMember = forMember)
    else -> idObjCName
  }

private val PrimitiveTypeDescriptor.primitiveObjCName: String
  get() =
    when (this) {
      PrimitiveTypes.VOID -> "void"
      PrimitiveTypes.BOOLEAN -> "boolean"
      PrimitiveTypes.BYTE -> "byte"
      PrimitiveTypes.SHORT -> "short"
      PrimitiveTypes.INT -> "int"
      PrimitiveTypes.LONG -> "long"
      PrimitiveTypes.CHAR -> "char"
      PrimitiveTypes.FLOAT -> "float"
      PrimitiveTypes.DOUBLE -> "double"
      else -> throw InternalCompilerError("Unexpected ${this::class.java.simpleName}")
    }

private fun DeclaredTypeDescriptor.declaredObjCName(useId: Boolean, forMember: Boolean): String =
  if (useId && isJavaLangObject(this)) idObjCName
  else typeDeclaration.objCName(forMember = forMember)

private fun ArrayTypeDescriptor.arrayObjCName(forMember: Boolean): String =
  leafTypeDescriptor.objCName(useId = false, forMember = forMember) + "Array" + dimensionsSuffix

private val ArrayTypeDescriptor.dimensionsSuffix
  get() = if (dimensions > 1) "$dimensions" else ""

private fun TypeVariable.variableObjCName(useId: Boolean, forMember: Boolean): String =
  upperBoundTypeDescriptor.objCName(useId = useId, forMember = forMember)

private val Variable.objCName
  get() = typeDescriptor.objCName(useId = true, forMember = true).titleCase

internal val FieldDescriptor.objCName: String
  get() = name!!.objCName.escapeJ2ObjCKeyword.escapeReservedObjCPrefixWith("the")
