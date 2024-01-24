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

import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.HasName
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.backend.kotlin.common.orIfNull
import com.google.j2cl.transpiler.backend.kotlin.source.Source

/**
 * Renderer of Kotlin names, with import resolution and alias generation.
 *
 * @property environment rendering environment
 * @property localTypeNameMap a map from local names to qualified names
 * @property localFieldNames a set of local field names
 */
internal data class NameRenderer(
  val environment: Environment,
  val localTypeNameMap: Map<String, String> = mapOf(),
  val localFieldNames: Set<String> = setOf(),
) {
  fun plusLocalNames(type: Type): NameRenderer =
    plusLocalTypeNameMap(type.localTypeNameMap).plusLocalFieldNames(type.localFieldNames)

  fun plusLocalTypeNameMap(localNameMap: Map<String, String>): NameRenderer =
    copy(localTypeNameMap = this.localTypeNameMap + localNameMap)

  fun plusLocalFieldNames(localNames: Set<String>): NameRenderer =
    copy(localFieldNames = this.localFieldNames + localNames)

  /** Returns source containing name of the given node. */
  fun nameSource(hasName: HasName) = identifierSource(environment.identifier(hasName))

  /**
   * Returns source for top-level qualified name.
   *
   * @param qualifiedName top-level qualified name
   */
  fun topLevelQualifiedNameSource(qualifiedName: String): Source =
    topLevelSimpleNameOrNull(qualifiedName)
      ?.let { identifierSource(it) }
      .orIfNull { qualifiedIdentifierSource(qualifiedName) }

  /**
   * Returns simple name to use for the given top-level qualified name, or null if using simple name
   * is not valid.
   *
   * @param qualifiedName top-level qualified name
   * @return simple name or null
   */
  private fun topLevelSimpleNameOrNull(qualifiedName: String): String? =
    qualifiedName.qualifiedNameToSimpleName().let { simpleName ->
      when {
        // Simple name shadowed by a field name.
        localFieldNames.contains(simpleName) -> null

        // Simple name shadowed by name in the environment.
        environment.containsIdentifier(simpleName) -> null

        // Look for local name, and use it if it refers to the same qualified name.
        // Otherwise, take local name from environment, which will resolve imports.
        else ->
          localTypeNameMap[simpleName].let { localQualifiedName ->
            if (localQualifiedName != null) {
              simpleName.takeIf { localQualifiedName == qualifiedName }
            } else {
              environment.qualifiedToNonAliasedSimpleName(qualifiedName)
            }
          }
      }
    }

  /**
   * Returns source from the given function adding the required opt-in.
   *
   * @param optInQualifiedName qualified name of the opt-in
   * @param fn function returning source which requires the given opt-in
   */
  fun sourceWithOptInQualifiedName(
    optInQualifiedName: String,
    fn: NameRenderer.() -> Source,
  ): Source = fn().also { environment.addOptInQualifiedName(optInQualifiedName) }

  /** Returns source for the given qualified name of extension member. */
  fun extensionMemberQualifiedNameSource(qualifiedName: String): Source =
    identifierSource(environment.qualifiedToSimpleName(qualifiedName))

  /**
   * Returns source containing qualified name for the given type descriptor.
   *
   * @param typeDescriptor the type descriptor to get the source for
   * @param asSuperType whether to use bridge name for the super-type
   */
  fun qualifiedNameSource(typeDescriptor: TypeDescriptor, asSuperType: Boolean = false): Source =
    if (typeDescriptor is DeclaredTypeDescriptor) {
      val typeDeclaration = typeDescriptor.typeDeclaration
      val enclosingTypeDescriptor = typeDescriptor.enclosingTypeDescriptor
      val nativeQualifiedName = typeDeclaration.ktNativeQualifiedName
      val bridgeQualifiedName = typeDeclaration.ktBridgeQualifiedName
      when {
        asSuperType ->
          // Use fully-qualified bridge name if present
          bridgeQualifiedName?.let { topLevelQualifiedNameSource(it) }
            ?: qualifiedNameSource(typeDescriptor)
        typeDeclaration.isLocal ->
          // Use simple name for local types
          identifierSource(typeDescriptor.typeDeclaration.ktSimpleName())
        nativeQualifiedName != null ->
          // Use fully-qualified native name if present
          topLevelQualifiedNameSource(nativeQualifiedName)
        enclosingTypeDescriptor != null ->
          // Use fully-qualified name for top-level type, and simple name for inner types
          Source.dotSeparated(
            qualifiedNameSource(enclosingTypeDescriptor),
            identifierSource(typeDeclaration.ktSimpleName()),
          )
        else -> topLevelQualifiedNameSource(typeDescriptor.ktQualifiedName)
      }
    } else {
      topLevelQualifiedNameSource(typeDescriptor.ktQualifiedName)
    }
}
