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
import com.google.j2cl.transpiler.ast.TypeDeclaration
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
  renderTypeDeclaration(type.declaration)

  renderSuperTypes(type)

  render(" ")
  renderInCurlyBrackets { renderTypeBody(type) }
}

fun Renderer.renderTypeDeclaration(declaration: TypeDeclaration) {
  render(declaration.classComponents.last().identifierSourceString)
  declaration.typeParameterDescriptors.takeIf { it.isNotEmpty() }?.let { typeVariables ->
    renderInAngleBrackets {
      renderCommaSeparated(typeVariables) { typeVariable ->
        render(typeVariable.declarationSourceString)
      }
    }
  }
}

private fun Renderer.renderSuperTypes(type: Type) {
  val hasConstructors = type.constructors.isNotEmpty()
  type
    .superTypesStream
    .filter { !isJavaLangObject(it) }
    .map { it.toNonNullable().sourceString.plus(if (it.isClass && !hasConstructors) "()" else "") }
    .collect(Collectors.joining(", "))
    .let { if (it.isNotEmpty()) render(": $it") }
}

private fun Renderer.renderTypeBody(type: Type) {
  // TODO(b/399455906): Remove short term hack to pull static methods into companion object.
  // TODO(b/399455906): Render enum values.
  val (staticMembers, instanceMembers) = type.members.partition { it.isStatic }

  val renderInstanceMembers = instanceMembers.isNotEmpty()
  if (renderInstanceMembers) {
    renderNewLine()
    renderSeparatedWithEmptyLine(instanceMembers) { renderMember(it, type.kind) }
  }

  if (staticMembers.isNotEmpty()) {
    renderNewLine()
    if (renderInstanceMembers) renderNewLine() // Empty line after last instance member.
    render("companion object ")
    renderInCurlyBrackets {
      renderNewLine()
      renderSeparatedWithEmptyLine(staticMembers) { renderMember(it, type.kind) }
    }
  }
}
