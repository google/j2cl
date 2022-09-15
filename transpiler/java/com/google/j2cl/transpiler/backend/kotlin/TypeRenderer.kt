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
import com.google.j2cl.transpiler.ast.NewInstance
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangEnum
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import java.util.stream.Collectors

fun Renderer.renderType(type: Type) {
  // Don't render KtNative types. We should never see them except readables.
  if (type.declaration.isKtNative) {
    render("// native class ")
    renderIdentifier(type.declaration.ktSimpleName)
    return
  }

  if (type.isClass && !type.declaration.isFinal) {
    if (type.declaration.isAbstract) render("abstract ") else render("open ")
  }
  if (
    type.enclosingTypeDeclaration != null &&
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
  renderIdentifier(declaration.ktSimpleName)
  declaration.directlyDeclaredTypeParameterDescriptors
    .takeIf { it.isNotEmpty() && it.all { it.isDenotable } }
    ?.let { parameters -> renderTypeParameters(parameters) }
}

private fun Renderer.renderSuperTypes(type: Type) {
  val superTypes =
    type.superTypesStream
      .filter { !isJavaLangObject(it) }
      .filter { !isJavaLangEnum(it) }
      .collect(Collectors.toList())
  if (superTypes.isNotEmpty()) {
    val hasConstructors = type.constructors.isNotEmpty()
    render(": ")
    renderCommaSeparated(superTypes) { superType ->
      renderTypeDescriptor(superType.toNonNullable().toNonRaw(), asSuperType = true)
      if (superType.isClass && !hasConstructors) render("()")
    }
  }
}

internal fun Renderer.renderTypeBody(type: Type) {
  copy(currentTypeDescriptor = type.typeDescriptor.toNonNullable()).run {
    render(" ")
    renderInCurlyBrackets {
      if (type.isEnum) {
        renderEnumValues(type)
      }

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
        renderSeparatedWithEmptyLine(instanceMembers) { renderMember(it) }
      }

      staticMembers = staticMembers.filter { !it.isEnumField }
      val renderCompanionObject = staticMembers.isNotEmpty()
      if (renderCompanionObject) {
        renderNewLine()
        if (renderInstanceMembers) renderNewLine() // Empty line after last instance member.
        render("companion object ")
        renderInCurlyBrackets {
          renderNewLine()
          renderSeparatedWithEmptyLine(staticMembers) { renderMember(it) }
        }
      }

      if (type.types.isNotEmpty()) {
        renderNewLine()
        if (renderInstanceMembers || renderCompanionObject) renderNewLine()
        renderSeparatedWithEmptyLine(type.types) { renderType(it) }
      }
    }
  }
}

private fun Renderer.renderEnumValues(type: Type) {
  renderNewLine()
  renderSeparatedWith(type.enumFields, ",\n") { field ->
    renderIdentifier(field.descriptor.name!!)
    val newInstance = field.initializer as NewInstance

    if (newInstance.arguments.isNotEmpty()) {
      renderInvocationArguments(newInstance)
    }

    newInstance.anonymousInnerClass?.let { renderTypeBody(it) }
  }
  render(";\n")
}
