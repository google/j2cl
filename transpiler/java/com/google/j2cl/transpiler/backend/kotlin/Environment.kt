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

/** Code generation environment. */
data class Environment(
  /** Name to identifier mapping. */
  private val nameToIdentifierMap: Map<HasName, String> = emptyMap(),

  /** A set of used identifiers, which potentially shadow imports. */
  private val identifierSet: Set<String> = emptySet(),

  /** Mutable map from simple name to qualified name of types to be imported. */
  val importedSimpleNameToQualifiedNameMap: MutableMap<String, String> = mutableMapOf(),

  /** Mutable map with imported classes for OptIn annotation. */
  val importedOptInQualifiedNames: MutableSet<String> = mutableSetOf()
) {
  /** Returns identifier for the given name */
  fun identifier(hasName: HasName): String =
    nameToIdentifierMap[hasName] ?: error("No such identifier: $hasName")

  /** Returns whether the given identifier is used. */
  fun containsIdentifier(identifier: String): Boolean = identifierSet.contains(identifier)
}
