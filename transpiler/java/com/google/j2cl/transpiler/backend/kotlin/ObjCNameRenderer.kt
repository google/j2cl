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
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.ast.Visibility

private const val INIT_WITH_PREFIX = "initWith"

internal val Visibility.needsObjCNameAnnotation
  get() = this == Visibility.PUBLIC || this == Visibility.PROTECTED

internal fun Renderer.renderOptInExperimentalObjCNameFileAnnotation() {
  render("@file:")
  renderQualifiedName("kotlin.OptIn")
  renderInParentheses {
    renderQualifiedName("kotlin.experimental.ExperimentalObjCName")
    render("::class")
  }
  renderNewLine()
}

internal fun Renderer.renderObjCNameAnnotation(typeDeclaration: TypeDeclaration) {
  render("@")
  renderQualifiedName("kotlin.native.ObjCName")
  renderInParentheses {
    renderString(typeDeclaration.objCName)
    render(", exact = true")
  }
  renderNewLine()
}

internal fun Renderer.renderObjCNameAnnotation(objCName: String) {
  render("@")
  renderQualifiedName("kotlin.native.ObjCName")
  renderInParentheses {
    renderString(objCName)
    render(", exact = false")
  }
  renderNewLine()
}

internal fun Renderer.renderObjCParameterNameAnnotation(renamedParam: String?) {
  if (renamedParam != null) {
    render("@")
    renderQualifiedName("kotlin.native.ObjCName")
    renderInParentheses {
      renderString(renamedParam)
      render(", exact = false")
    }
    render(" ")
  }
}

internal fun Method.getObjCMethodName(): String {
  val (objCName, _) = computeObjcMethodComponents()
  return objCName!!
}

internal fun Method.getObjCParameterNames(): List<String>? {
  val (_, objCParameterNames) = computeObjcNameComponents()
  return objCParameterNames
}

/**
 * Parses ObjectiveCName annotation, extracting method name and parameters to be used in the
 * ObjCName annotation kotlin requires.
 */
private fun Method.computeObjcNameComponents(): Pair<String?, List<String>?> {
  if (!needsObjCNameAnnotations) return Pair(null, null)
  else if (descriptor.isConstructor) return computeObjCConstructorComponents()
  else return computeObjcMethodComponents()
}

/**
 * Parses constructor ObjectiveCName annotations for which only parameter annotations need to be
 * outputted. Constructor annotations are expected to begin with "initWith" and the first parameter
 * should always exclude "with" prefix since it is automatically generated.
 */
private fun Method.computeObjCConstructorComponents(): Pair<String?, List<String>> {
  var objCParameterNames = mutableListOf<String>()
  var objCName = descriptor.getObjectiveCName()

  if (parameters.isNotEmpty()) {
    if (objCName != null) {
      objCParameterNames = objCName.split(":").toMutableList()
      if (objCParameterNames[0].startsWith(INIT_WITH_PREFIX))
        objCParameterNames[0] = objCParameterNames[0].substring(INIT_WITH_PREFIX.length)
      else
        objCParameterNames[0] = "${parameters[0].typeDescriptor.objCName(useId = true).titleCase}"
    } else {
      objCParameterNames =
        parameters
          .map { "with${it.typeDescriptor.objCName(useId = true).titleCase}" }
          .toMutableList()
      objCParameterNames[0] = objCParameterNames[0].substring(4)
    }
  }
  return Pair(objCName, objCParameterNames)
}

/**
 * Computes method and parameter annotation names from the ObjectiveCName annotation which is in
 * ObjC style with method name and parameters delimited by colons, and the first parameter left
 * unnamed. However, the first element in the delimited string needs to be split into the method
 * name and first parameter name because in Kotlin, ObjC annotations are required for each element.
 */
private fun Method.computeObjcMethodComponents(): Pair<String?, List<String>> {
  var objCParameterNames = mutableListOf<String>()
  var objCName = descriptor.getObjectiveCName()
  if (parameters.isNotEmpty()) {
    if (objCName != null) {
      val methodName = descriptor.name!!
      val prefix = methodName.commonPrefixWith(objCName)
      objCParameterNames = objCName.split(":").toMutableList()

      // If possible, split method name and first parameter by shared prefix
      if (prefix.isNotEmpty() && prefix.length != objCParameterNames[0].length) {
        objCName = prefix
        objCParameterNames[0] = objCParameterNames[0].substring(prefix.length)
      }
      // Otherwise, split string in half arbitrarily. Does not handle single character objc name
      else {
        check(objCParameterNames[0].length > 1)
        val midIndex = (objCParameterNames[0].length / 2).toInt()
        objCName = objCParameterNames[0].substring(0, midIndex)
        objCParameterNames[0] = objCParameterNames[0].substring(midIndex)
      }
    } else {
      objCParameterNames =
        parameters
          .map { "with${it.typeDescriptor.objCName(useId = true).titleCase}" }
          .toMutableList()
    }
  }

  return Pair(objCName, objCParameterNames)
}

internal val TypeDeclaration.objCName: String
  get() = objectiveCName ?: mappedObjCName ?: defaultObjCName

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
  get() =
    simpleObjCName.let { simpleObjCName ->
      enclosingTypeDeclaration?.let { it.objCName + "_" + simpleObjCName }
        ?: objectiveCNamePrefix?.let { it + simpleObjCName }
        ?: (objCPackagePrefix + simpleObjCName)
    }

private val TypeDeclaration.simpleObjCName: String
  get() = simpleSourceName.objCName

private val TypeDeclaration.objCPackagePrefix: String
  get() = packageName?.split('.')?.joinToString(separator = "") { it.titleCase.objCName } ?: ""

private val String.titleCase
  get() = StringUtils.capitalize(this)

private val String.objCName
  get() = replace('$', '_')

private fun TypeDescriptor.objCName(useId: Boolean): String =
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
      leafTypeDescriptor.objCName(useId = false) + "Array" + dimensionsSuffix
    is DeclaredTypeDescriptor ->
      if (useId && isJavaLangObject(this)) "id" else typeDeclaration.objCName
    is TypeVariable -> upperBoundTypeDescriptor.objCName(useId = useId)
    else -> "id"
  }

private val ArrayTypeDescriptor.dimensionsSuffix
  get() = if (dimensions > 1) "$dimensions" else ""
