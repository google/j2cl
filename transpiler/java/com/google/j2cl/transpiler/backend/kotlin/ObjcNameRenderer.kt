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
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptor

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

internal fun Renderer.renderObjCNameAnnotation(parameterDescriptor: ParameterDescriptor) {
  render("@")
  renderQualifiedName("kotlin.native.ObjCName")
  renderInParentheses {
    render("\"with")
    render(parameterDescriptor.getTypeDescriptor().objCName.titleCase)
    render("\", exact = false")
  }
  render(" ")
}

private val TypeDeclaration.objCName: String
  get() = objCPackagePrefix + objCSimpleName

private val TypeDeclaration.objCPackagePrefix: String
  get() = (packageName ?: "").split('.').map { it.titleCase }.joinToString(separator = "")

private val TypeDeclaration.objCSimpleName: String
  get() = ktSimpleName

private val String.titleCase
  get() = StringUtils.capitalize(this)

private val TypeDescriptor.objCName: String
  get() =
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
          // TODO(litstrong): figure out how to hanlde Void or void
          else -> throw InternalCompilerError("Unexpected ${this::class.java.simpleName}")
        }
      }
      is ArrayTypeDescriptor ->
        leafTypeDescriptor.objCName + "Array" + (if (dimensions > 1) "${dimensions}" else "")
      is DeclaredTypeDescriptor ->
        (if (hasTypeArguments()) "id" else typeDeclaration.objCName.titleCase)
      else -> "id"
    }
