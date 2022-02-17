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
import com.google.j2cl.transpiler.ast.TypeVariable
import java.util.stream.Collectors

fun Renderer.renderType(type: Type) {
  if (type.isClass && !type.declaration.isFinal) {
    if (type.declaration.isAbstract) render("abstract ") else render("open ")
  }
  if (type.enclosingTypeDeclaration != null &&
      type.kind == Kind.CLASS &&
      !type.isStatic &&
      !type.declaration.isLocal
  ) {
    render("inner ")
  }
  render(
    when (type.kind) {
      Kind.CLASS -> "class "
      Kind.ENUM -> "enum class "
      Kind.INTERFACE -> (if (type.declaration.isFunctionalInterface) "fun " else "") + "interface "
    }
  )
  renderTypeDeclaration(type.declaration)

  renderSuperTypes(type)
  renderWhereClause(type.declaration.typeParameterDescriptors)
  renderTypeBody(type)
}

fun Renderer.renderTypeDeclaration(declaration: TypeDeclaration) {
  renderIdentifier(declaration.classComponents.last())
  declaration.renderedTypeParameterDescriptors.takeIf { it.isNotEmpty() }?.let { parameters ->
    renderTypeParameters(parameters)
  }
}

private fun Renderer.renderSuperTypes(type: Type) {
  val superTypes =
    type.superTypesStream.filter { !isJavaLangObject(it) }.collect(Collectors.toList())
  if (superTypes.isNotEmpty()) {
    val hasConstructors = type.constructors.isNotEmpty()
    render(": ")
    renderCommaSeparated(superTypes) { superType ->
      renderTypeDescriptor(superType.toNonNullable(), projectBounds = true)
      if (superType.isClass && !hasConstructors) render("()")
    }
  }
}

internal fun Renderer.renderTypeBody(type: Type) {
  render(" ")
  renderInCurlyBrackets {
    // TODO(b/399455906): Remove short term hack to pull static methods into companion object.
    var (staticMembers, instanceMembers) = type.members.partition { it.isStatic }

    // Don't render constructors for anonymous classes.
    // TODO(b/210670710): Remove when anonymous constructors are no longer synthesized.
    if (type.declaration.isAnonymous) {
      instanceMembers = instanceMembers.filter { !it.isConstructor }
    }

    val renderInstanceMembers = instanceMembers.isNotEmpty()
    if (renderInstanceMembers) {
      renderNewLine()
      renderSeparatedWithEmptyLine(instanceMembers) { renderMember(it, type.kind) }
    }

    val renderCompanionObject = staticMembers.isNotEmpty()
    if (renderCompanionObject) {
      renderNewLine()
      if (renderInstanceMembers) renderNewLine() // Empty line after last instance member.
      render("companion object ")
      renderInCurlyBrackets {
        renderNewLine()
        renderSeparatedWithEmptyLine(staticMembers) { renderMember(it, type.kind) }
      }
    }

    if (type.types.isNotEmpty()) {
      renderNewLine()
      if (renderInstanceMembers || renderCompanionObject) renderNewLine()
      renderSeparatedWithEmptyLine(type.types) { renderType(it) }
    }
  }
}

// TODO(b/216796920): Remove when the bug is fixed.
internal val TypeDeclaration.renderedTypeParameterDescriptors: List<TypeVariable>
  get() {
    val enclosingTypeDeclaration = enclosingTypeDeclaration
    return if (enclosingTypeDeclaration == null || !isCapturingEnclosingInstance)
      typeParameterDescriptors
    else typeParameterDescriptors.dropLast(enclosingTypeDeclaration.typeParameterDescriptors.size)
  }
