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

import com.google.j2cl.transpiler.ast.Kind
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import java.util.stream.Collectors

fun Renderer.renderType(type: Type) {
  if (type.isClass && !type.declaration.isFinal) {
    if (type.declaration.isAbstract) render("abstract ") else render("open ")
  }
  render(
    when (type.kind) {
      Kind.CLASS -> "class "
      Kind.ENUM -> "enum class "
      Kind.INTERFACE -> "interface "
    }
  )
  render(type.declaration.classComponents.last().identifierSourceString)

  renderSuperTypes(type)

  render(" ")
  renderInCurlyBrackets { renderTypeBody(type) }
}

private fun Renderer.renderSuperTypes(type: Type) {
  type
    .superTypesStream
    .filter { !isJavaLangObject(it) }
    .map { it.toNonNullable().sourceString }
    .collect(Collectors.joining(", "))
    .let { if (it.isNotEmpty()) render(": $it") }
}

private fun Renderer.renderTypeBody(type: Type) {
  // TODO(b/399455906): add support for field declarations
  // TODO(b/399455906): Remove short term hack to pull static methods into companion object.
  // TODO(b/399455906): Render enum values.
  val (staticMethods, instanceMethods) = type.methods.partition { it.isStatic }

  if (instanceMethods.isNotEmpty()) {
    renderNewLine()
    renderSeparatedWithEmptyLine(instanceMethods) { renderMethod(it) }
  }

  if (staticMethods.isNotEmpty()) {
    renderNewLine()
    if (instanceMethods.isNotEmpty()) renderNewLine() // Empty line after last method.
    render("companion object ")
    renderInCurlyBrackets {
      renderNewLine()
      renderSeparatedWithEmptyLine(staticMethods) { renderMethod(it) }
    }
  }
}
