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
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.ast.Visibility

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
    renderString(typeDeclaration.objCName(useId = true))
    render(", exact = true")
  }
  renderNewLine()
}

internal fun Renderer.renderObjCNameAnnotation(methodDescriptor: MethodDescriptor) {
  var objCName = methodDescriptor.getObjectiveCName()
  // TODO(chenmorris): support the case that one method has >0 params
  if (objCName != null && !objCName.contains(":")) {
    render("@")
    renderQualifiedName("kotlin.native.ObjCName")
    renderInParentheses {
      renderString(objCName)
      render(", exact = false")
    }
    renderNewLine()
  }
}

internal fun Renderer.renderObjCNameAnnotation(parameterDescriptor: ParameterDescriptor) {
  render("@")
  renderQualifiedName("kotlin.native.ObjCName")
  renderInParentheses {
    renderString("with${parameterDescriptor.typeDescriptor.objCName(useId = true).titleCase}")
    render(", exact = false")
  }
  render(" ")
}

private fun TypeDeclaration.objCName(useId: Boolean): String =
  when (qualifiedBinaryName) {
    "java.lang.Object" -> if (useId) "id" else "NSObject"
    "java.lang.String" -> "NSString"
    "java.lang.Class" -> "IOSClass"
    "java.lang.Number" -> "NSNumber"
    "java.lang.Cloneable" -> "NSCopying"
    else -> getObjectiveCName() ?: (objCPackagePrefix + objCSimpleName)
  }

private val TypeDeclaration.objCPackagePrefix: String
  get() =
    (packageName ?: "").split('.').map { it.titleCase.toObjCName }.joinToString(separator = "")

private val TypeDeclaration.objCSimpleName: String
  get() = (enclosingTypeDeclaration?.objCSimpleName?.plus("_") ?: "") + simpleSourceName.toObjCName

private val String.titleCase
  get() = StringUtils.capitalize(this)

private val String.toObjCName
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
        // TODO(litstrong): figure out how to handle Void or void
        else -> throw InternalCompilerError("Unexpected ${this::class.java.simpleName}")
      }
    }
    is ArrayTypeDescriptor ->
      leafTypeDescriptor.objCName(useId = false) + "Array" + dimensionsSuffix
    is DeclaredTypeDescriptor -> typeDeclaration.objCName(useId = useId)
    is TypeVariable -> upperBoundTypeDescriptor.objCName(useId = useId)
    else -> "id"
  }

private val ArrayTypeDescriptor.dimensionsSuffix
  get() = if (dimensions > 1) "$dimensions" else ""
