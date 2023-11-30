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
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.annotation
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.assignment
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.literal
import com.google.j2cl.transpiler.backend.kotlin.ast.CompanionDeclaration
import com.google.j2cl.transpiler.backend.kotlin.ast.CompanionObject
import com.google.j2cl.transpiler.backend.kotlin.ast.declaration
import com.google.j2cl.transpiler.backend.kotlin.common.letIf
import com.google.j2cl.transpiler.backend.kotlin.common.mapFirst
import com.google.j2cl.transpiler.backend.kotlin.common.titleCased
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.orEmpty

internal fun Renderer.objCNameAnnotationSource(name: String, exact: Boolean? = null): Source =
  annotation(
    topLevelQualifiedNameSource(
      "kotlin.native.ObjCName",
      optInQualifiedName = "kotlin.experimental.ExperimentalObjCName"
    ),
    literal(name),
    exact?.let { parameterSource("exact", literal(it)) }.orEmpty()
  )

internal fun Renderer.objCAnnotationSource(typeDeclaration: TypeDeclaration): Source =
  Source.emptyUnless(typeDeclaration.needsObjCNameAnnotation) {
    objCNameAnnotationSource(typeDeclaration.objCName, exact = true)
  }

internal fun Renderer.objCAnnotationSource(companionObject: CompanionObject): Source =
  Source.emptyUnless(companionObject.needsObjCNameAnnotation) {
    objCNameAnnotationSource(companionObject.declaration.objCName, exact = true)
  }

internal fun Renderer.objCAnnotationSource(
  methodDescriptor: MethodDescriptor,
  methodObjCNames: MethodObjCNames?
): Source =
  Source.emptyUnless(!methodDescriptor.isConstructor) {
    methodObjCNames?.methodName?.let { objCNameAnnotationSource(it) }.orEmpty()
  }

internal fun Renderer.objCAnnotationSource(fieldDescriptor: FieldDescriptor): Source =
  Source.emptyUnless(fieldDescriptor.needsObjCNameAnnotations) {
    objCNameAnnotationSource(fieldDescriptor.objCName)
  }

private fun parameterSource(name: String, valueSource: Source): Source =
  assignment(source(name), valueSource)

/** The names are mangled according to J2ObjC rules. */
internal data class MethodObjCNames(val methodName: String, val parameterNames: List<String>)

internal fun Method.toObjCNames(): MethodObjCNames? =
  when {
    !descriptor.needsObjCNameAnnotations -> null
    descriptor.isConstructor -> toConstructorObjCNames()
    else -> toNonConstructorObjCNames()
  }

private val MethodDescriptor.needsObjCNameAnnotations: Boolean
  get() =
    enclosingTypeDescriptor.typeDeclaration.let { enclosingTypeDeclaration ->
      !enclosingTypeDeclaration.isLocal &&
        !enclosingTypeDeclaration.isAnonymous &&
        visibility.needsObjCNameAnnotation &&
        !isKtOverride
    }

private val FieldDescriptor.needsObjCNameAnnotations: Boolean
  get() =
    enclosingTypeDescriptor.typeDeclaration.let { enclosingTypeDeclaration ->
      enclosingTypeDeclaration.visibility.needsObjCNameAnnotation &&
        !enclosingTypeDeclaration.isLocal &&
        !enclosingTypeDeclaration.isAnonymous &&
        visibility.needsObjCNameAnnotation
    }

private val TypeDeclaration.needsObjCNameAnnotation: Boolean
  get() = visibility.needsObjCNameAnnotation && !isLocal && !isAnonymous

private val CompanionObject.needsObjCNameAnnotation
  get() = enclosingTypeDeclaration.needsObjCNameAnnotation

private val Visibility.needsObjCNameAnnotation
  get() = this == Visibility.PUBLIC || this == Visibility.PROTECTED

private fun Method.toConstructorObjCNames(): MethodObjCNames =
  descriptor.objectiveCName.let { objectiveCName ->
    MethodObjCNames(
      "init",
      if (objectiveCName != null) {
        objectiveCName.objCMethodParameterNames.mapFirst {
          val prefix = "initWith"
          if (it.startsWith(prefix)) {
            it.substring(prefix.length)
          } else {
            parameters.first().objCName
          }
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
      MethodObjCNames(
        objectiveCName ?: descriptor.ktName.escapeJ2ObjCKeyword,
        parameters.map { "with${it.objCName}" }
      )
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

private const val objCTypeNamePrefix: String = "J2kt"

internal val TypeDeclaration.objCName: String
  get() = objCTypeNamePrefix + objCNameWithoutPrefix

internal val TypeDeclaration.objCNameWithoutPrefix: String
  get() = mappedObjCName ?: nonMappedObjCName

private val String.objCCompanionTypeName: String
  get() = this + "Companion"

internal val CompanionDeclaration.objCName
  get() = enclosingTypeDeclaration.objCName.objCCompanionTypeName

internal val CompanionDeclaration.objCNameWithoutPrefix
  get() = enclosingTypeDeclaration.objCNameWithoutPrefix.objCCompanionTypeName

private val TypeDeclaration.nonMappedObjCName: String
  get() = objectiveCName ?: defaultObjCName

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

private val TypeDeclaration.defaultObjCName: String
  get() = objCNamePrefix + simpleObjCName

private val TypeDeclaration.objCNamePrefix: String
  get() =
    enclosingTypeDeclaration.run {
      if (this != null) {
        objCNameWithoutPrefix + "_"
      } else {
        simpleObjCNamePrefix
      }
    }

private val TypeDeclaration.simpleObjCNamePrefix: String
  get() = objectiveCNamePrefix ?: objCPackagePrefix

private val TypeDeclaration.simpleObjCName: String
  get() = simpleSourceName.objCName

private val TypeDeclaration.objCPackagePrefix: String
  get() = packageName?.objCPackagePrefix ?: ""

private val String.objCPackagePrefix: String
  get() = split('.').joinToString(separator = "") { it.titleCased.objCName }

internal val String.objCName
  get() = replace('$', '_')

private const val idObjCName = "id"

internal fun TypeDescriptor.objCName(useId: Boolean): String =
  when (this) {
    is PrimitiveTypeDescriptor -> primitiveObjCName
    is ArrayTypeDescriptor -> arrayObjCName
    is DeclaredTypeDescriptor -> declaredObjCName(useId = useId)
    is TypeVariable -> variableObjCName(useId = useId)
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

private fun DeclaredTypeDescriptor.declaredObjCName(useId: Boolean): String =
  if (useId && isJavaLangObject(this)) {
    idObjCName
  } else {
    typeDeclaration.objCNameWithoutPrefix
  }

private val ArrayTypeDescriptor.arrayObjCName: String
  get() = leafTypeDescriptor.objCName(useId = false) + "Array" + dimensionsSuffix

private val ArrayTypeDescriptor.dimensionsSuffix: String
  get() = if (dimensions > 1) "$dimensions" else ""

private fun TypeVariable.variableObjCName(useId: Boolean): String =
  upperBoundTypeDescriptor.objCName(useId = useId)

private val Variable.objCName: String
  get() = typeDescriptor.objCName(useId = true).titleCased

internal val FieldDescriptor.objCName: String
  get() = name!!.objCName.escapeJ2ObjCKeyword.letIf(!isEnumConstant) { it + "_" }
