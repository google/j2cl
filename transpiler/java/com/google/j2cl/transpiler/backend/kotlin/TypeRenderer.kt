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

import com.google.j2cl.transpiler.ast.NewInstance
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangEnum
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.backend.kotlin.ast.kotlinMembers

fun Renderer.renderType(type: Type) {
  val typeDeclaration = type.declaration

  // Don't render KtNative types. We should never see them except readables.
  if (typeDeclaration.isKtNative) {
    render("// native class ")
    renderIdentifier(typeDeclaration.ktSimpleName)
    return
  }

  if (typeDeclaration.visibility.needsObjCNameAnnotation && !typeDeclaration.isLocal) {
    renderObjCNameAnnotation(typeDeclaration.objCName, exact = true)
    renderNewLine()
  }

  if (type.isClass && !typeDeclaration.isFinal) {
    if (typeDeclaration.isAbstract) render("abstract ") else render("open ")
  }
  if (
    typeDeclaration.enclosingTypeDeclaration != null &&
      type.kind == Kind.CLASS &&
      !type.isStatic &&
      !typeDeclaration.isLocal
  ) {
    render("inner ")
  }
  render(
    when (type.kind) {
      Kind.CLASS -> "class "
      Kind.ENUM -> "enum class "
      Kind.INTERFACE -> (if (typeDeclaration.isKtFunctionalInterface) "fun " else "") + "interface "
    }
  )
  renderTypeDeclaration(typeDeclaration)

  renderSuperTypes(type)
  renderWhereClause(typeDeclaration.typeParameterDescriptors)
  renderTypeBody(type)
}

fun Renderer.renderTypeDeclaration(declaration: TypeDeclaration) {
  renderIdentifier(declaration.ktSimpleName)
  declaration.directlyDeclaredTypeParameterDescriptors
    .takeIf { it.isNotEmpty() }
    ?.let { parameters -> renderTypeParameters(parameters) }
}

private fun Renderer.renderSuperTypes(type: Type) {
  val superTypes =
    type.declaredSuperTypeDescriptors.filter { !isJavaLangObject(it) && !isJavaLangEnum(it) }
  if (superTypes.isNotEmpty()) {
    val hasConstructors = type.constructors.isNotEmpty()
    render(": ")
    renderCommaSeparated(superTypes) { superType ->
      renderTypeDescriptor(superType.toNonNullable(), asSuperType = true)
      if (superType.isClass && !hasConstructors) render("()")
    }
  }
}

internal fun Renderer.renderTypeBody(type: Type) {
  copy(
      currentType = type,
      renderThisReferenceWithLabel = false,
      localNames = localNames + type.localNames
    )
    .run {
      render(" ")
      renderInCurlyBrackets {
        if (type.isEnum) {
          renderEnumValues(type)
        }

        val kotlinMembers = type.kotlinMembers
        if (kotlinMembers.isNotEmpty()) {
          renderNewLine()
          renderSeparatedWithEmptyLine(kotlinMembers) { render(it) }
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
