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

import com.google.j2cl.transpiler.ast.HasName
import com.google.j2cl.transpiler.backend.kotlin.ast.isHardKeyword
import com.google.j2cl.transpiler.backend.kotlin.common.letIf
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.source

internal fun Renderer.nameSource(hasName: HasName) =
  identifierSource(environment.identifier(hasName))

internal fun identifierSource(identifier: String): Source = source(identifier.identifierString)

internal val String.identifierString
  get() =
    replace("$", "___").let { withoutDollars ->
      withoutDollars.letIf(isHardKeyword(withoutDollars) || !withoutDollars.isValidIdentifier) {
        withoutDollars.inBackTicks
      }
    }

internal val String.inBackTicks
  get() = "`$this`"

internal fun Renderer.topLevelQualifiedNameSource(
  qualifiedName: String,
  optInQualifiedName: String? = null
): Source =
  qualifiedToNonAliasedSimpleName(qualifiedName)
    .let { simpleName ->
      if (simpleName != null) identifierSource(simpleName)
      else qualifiedIdentifierSource(qualifiedName)
    }
    .also { optInQualifiedName?.let { environment.importedOptInQualifiedNames.add(it) } }

internal fun Renderer.extensionMemberQualifiedNameSource(qualifiedName: String): Source =
  identifierSource(qualifiedToSimpleName(qualifiedName))

internal fun Renderer.qualifiedToSimpleName(qualifiedName: String): String =
  qualifiedToNonAliasedSimpleName(qualifiedName) ?: qualifiedToAliasedSimpleName(qualifiedName)

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

internal fun qualifiedIdentifierSource(identifier: String): Source =
  dotSeparated(identifier.qualifiedNameComponents().map(::identifierSource))

private val String.isValidIdentifier: Boolean
  get() = first().isValidIdentifierFirstChar && all { it.isValidIdentifierChar }

private val Char.isValidIdentifierChar: Boolean
  get() = isLetterOrDigit() || this == '_'

private val Char.isValidIdentifierFirstChar: Boolean
  get() = isValidIdentifierChar && !isDigit()
