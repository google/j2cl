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
import com.google.j2cl.transpiler.backend.kotlin.ast.Import

/**
 * Code generation environment.
 *
 * @property nameToIdentifierMap a map from named node to rendered identifier string
 * @property identifierSet a set of used identifier strings, which potentially shadow imports
 * @property importedSimpleNameToQualifiedNameMutableMap a mutable map from simple name string to
 *   qualified name string of types to be imported, filled-in during code generation
 * @property importedOptInQualifiedNamesMutableSet a mutable set with imported qualified names for
 *   [@OptIn] annotation, filled-in during code generation
 * @property topLevelQualifiedNamesSet top-level qualified names, which will be rendered as simple
 *   name without import
 */
internal data class Environment(
  private val nameToIdentifierMap: Map<HasName, String> = emptyMap(),
  private val identifierSet: Set<String> = emptySet(),
  private val importedSimpleNameToQualifiedNameMutableMap: MutableMap<String, String> =
    mutableMapOf(),
  private val importedOptInQualifiedNamesMutableSet: MutableSet<String> = mutableSetOf(),
) {
  /** Returns identifier for the given named node. */
  fun identifier(hasName: HasName): String =
    nameToIdentifierMap[hasName] ?: error("No such identifier: $hasName")

  /** Returns whether the given identifier is used. */
  fun containsIdentifier(identifier: String): Boolean = identifierSet.contains(identifier)

  /** A set of collected imports. */
  val importsSet: Set<Import>
    get() =
      importedSimpleNameToQualifiedNameMutableMap.entries
        .map { (simpleName, qualifiedName) ->
          Import(
            qualifiedName.qualifiedNameComponents(),
            simpleName
              .takeUnless { it == qualifiedName.qualifiedNameToSimpleName() }
              ?.let { Import.Suffix.WithAlias(it) },
          )
        }
        .toSet()

  /** A set of collected opt-in qualified names. */
  val importedOptInQualifiedNamesSet: Set<String>
    get() = importedOptInQualifiedNamesMutableSet.toSet()

  /** Converts the given qualified name to a simple name, aliasing if necessary. */
  fun qualifiedToSimpleName(qualifiedName: String): String =
    qualifiedToNonAliasedSimpleName(qualifiedName) ?: qualifiedToAliasedSimpleName(qualifiedName)

  /** Converts the given qualified name to non-aliased simple name, or null if alias is required. */
  fun qualifiedToNonAliasedSimpleName(qualifiedName: String): String? {
    val simpleName = qualifiedName.qualifiedNameToSimpleName()
    val importMap = importedSimpleNameToQualifiedNameMutableMap
    val importedQualifiedName = importMap[simpleName]
    if (importedQualifiedName == null) {
      importMap[simpleName] = qualifiedName
      return simpleName
    }
    if (importedQualifiedName == qualifiedName) {
      return simpleName
    }
    return null
  }

  /** Adds the given opt-in qualified name. */
  fun addOptInQualifiedName(optInQualifiedName: String) {
    importedOptInQualifiedNamesMutableSet.add(optInQualifiedName)
  }

  private fun qualifiedToAliasedSimpleName(qualifiedName: String): String {
    return qualifiedName.qualifiedNameToAlias().also {
      importedSimpleNameToQualifiedNameMutableMap[it] = qualifiedName
    }
  }
}
