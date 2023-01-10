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

import com.google.j2cl.transpiler.backend.kotlin.ast.isForbiddenKeyword
import com.google.j2cl.transpiler.backend.kotlin.ast.isHardKeyword

internal fun Renderer.renderIdentifier(identifier: String) {
  render(identifier.identifierString)
}

// Dollar sign ($) is not a valid identifier character since Kotlin 1.7, as well as many other
// characters. For now, it is replaced with triple underscores (___) to minimise a risk of
// conflict.
// TODO(b/236360941): Implement escaping which would work across all platforms.
internal val String.identifierString
  get() =
    replace("$", "___").run {
      if (isForbiddenKeyword(this)) this + "__" // This needs to be __ for consistency with J2ObjC.
      else if (isHardKeyword(this) || !isValidIdentifier) "`$this`" else this
    }

internal fun Renderer.renderPackageName(packageName: String) {
  renderQualifiedIdentifier(packageName)
}

internal fun Renderer.renderTopLevelQualifiedName(qualifiedName: String) {
  val simpleName = qualifiedToNonAliasedSimpleName(qualifiedName)
  if (simpleName != null) {
    renderIdentifier(simpleName)
  } else {
    renderQualifiedIdentifier(qualifiedName)
  }
}

internal fun Renderer.renderExtensionMemberQualifiedName(qualifiedName: String) {
  renderIdentifier(qualifiedToSimpleName(qualifiedName))
}

internal fun Renderer.qualifiedToSimpleName(qualifiedName: String): String {
  return qualifiedToNonAliasedSimpleName(qualifiedName)
    ?: qualifiedToAliasedSimpleName(qualifiedName)
}

internal fun Renderer.qualifiedToNonAliasedSimpleName(qualifiedName: String): String? {
  val simpleName = qualifiedName.qualifiedNameToSimpleName()
  if (localNames.contains(simpleName) || environment.containsIdentifier(simpleName)) {
    return null
  }
  if (topLevelQualifiedNames.contains(qualifiedName)) {
    return simpleName
  }
  val importMap = environment.importedSimpleNameToQualifiedNameMap
  val importedQualifiedName = importMap[simpleName]
  if (importedQualifiedName == null) {
    if (topLevelQualifiedNames.any { it.qualifiedNameToSimpleName() == simpleName }) {
      return null
    }
    importMap[simpleName] = qualifiedName
    return simpleName
  }
  if (importedQualifiedName == qualifiedName) {
    return simpleName
  }
  return null
}

internal fun Renderer.qualifiedToAliasedSimpleName(qualifiedName: String): String {
  return qualifiedName.qualifiedNameToAlias().also {
    environment.importedSimpleNameToQualifiedNameMap[it] = qualifiedName
  }
}

private fun Renderer.renderQualifiedIdentifier(identifier: String) {
  renderDotSeparated(identifier.split('.')) { renderIdentifier(it) }
}

private val String.isValidIdentifier
  get() = first().isValidIdentifierFirstChar && all { it.isValidIdentifierChar }

private val Char.isValidIdentifierChar
  get() = isLetterOrDigit() || this == '_'

private val Char.isValidIdentifierFirstChar
  get() = isValidIdentifierChar && !isDigit()
