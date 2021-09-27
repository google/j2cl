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

import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDescriptors

fun Renderer.renderType(type: Type) {
  if (!type.declaration.isFinal) render("open ")
  render("class ${type.declaration.simpleSourceName}")

  // TODO(dpo): add support for class hierarchies
  renderTypeExtendsClause(type)

  render(" ")
  renderInCurlyBrackets { renderTypeBody(type) }
}

private fun Renderer.renderTypeExtendsClause(type: Type) {
  val superTypeDescriptor = type.superTypeDescriptor
  if (superTypeDescriptor != null && !TypeDescriptors.isJavaLangObject(superTypeDescriptor)) {
    render(" extends ${superTypeDescriptor.qualifiedSourceName}")
  }
}

private fun Renderer.renderTypeBody(type: Type) {
  // TODO(dpo): add support for field declarations
  // TODO(dpo): Remove short term hack to pull static methods into companion object.
  val (staticMethods, instanceMethods) = type.methods.partition { it.isStatic }

  instanceMethods.forEach { renderMethod(it) }

  if (staticMethods.isNotEmpty()) {
    renderNewLine()
    render("companion object ")
    renderInCurlyBrackets { type.methods.forEach { renderMethod(it) } }
  }
}
