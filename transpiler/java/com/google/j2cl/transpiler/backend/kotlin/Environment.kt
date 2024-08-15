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

import com.google.j2cl.transpiler.ast.AstUtils
import com.google.j2cl.transpiler.ast.FieldDescriptor
import com.google.j2cl.transpiler.ast.HasName
import com.google.j2cl.transpiler.ast.MemberDescriptor
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.ast.Visibility
import com.google.j2cl.transpiler.backend.kotlin.ast.Import
import com.google.j2cl.transpiler.backend.kotlin.ast.Visibility as KtVisibility
import com.google.j2cl.transpiler.backend.kotlin.ast.withWidestScopeOrNull

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
 * @property privateAsKtInternalDeclarationMemberDescriptorSet a set of private declaration member
 *   descriptors which should be rendered as internal in Kotlin.
 */
internal data class Environment(
  private val nameToIdentifierMap: Map<HasName, String> = emptyMap(),
  private val identifierSet: Set<String> = emptySet(),
  private val importedSimpleNameToQualifiedNameMutableMap: MutableMap<String, String> =
    mutableMapOf(),
  private val importedOptInQualifiedNamesMutableSet: MutableSet<String> = mutableSetOf(),
  private val privateAsKtInternalDeclarationMemberDescriptorSet: Set<MemberDescriptor> = setOf(),
  private val captureIndices: MutableMap<TypeVariable, Int> = mutableMapOf(),
) {
  /**
   * Returns identifier for the given named node. Use "_MISSING" prefix for missing names, to help
   * debugging.
   */
  fun identifier(hasName: HasName): String =
    nameToIdentifierMap[hasName] ?: "${hasName.name}_MISSING"

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

  /** Returns whether the given member descriptor should be rendered as private in Kotlin. */
  private fun isKtPrivate(memberDescriptor: MemberDescriptor): Boolean =
    memberDescriptor.declarationDescriptor.let {
      it.visibility.isPrivate && !privateAsKtInternalDeclarationMemberDescriptorSet.contains(it)
    }

  /** Returns Kotlin member visibility. */
  fun ktVisibility(memberDescriptor: MemberDescriptor): KtVisibility =
    when {
      // Enum constructors are implicitly private in Kotlin
      memberDescriptor.isEnumConstructor -> KtVisibility.PRIVATE
      // All interface methods are public in Kotlin, and Java allows non-public static members, so
      // we map them to public.
      memberDescriptor.isInterfaceMethod -> KtVisibility.PUBLIC
      // Explicit private members.
      isKtPrivate(memberDescriptor) -> KtVisibility.PRIVATE
      // Use default visibility for everything else.
      else -> memberDescriptor.visibility!!.defaultMemberKtVisibility
    }

  /** Returns Kotlin type visibility. */
  fun ktVisibility(typeDeclaration: TypeDeclaration): KtVisibility =
    // Render all types as public, to allow extending with wider visibility which is legal in Java,
    // but illegal in Kotlin.
    // TODO(b/358052247): Render private types as private if possible
    KtVisibility.PUBLIC

  /**
   * Inferred visibility, which does not require explicit visibility modifier in the source code.
   */
  fun inferredKtVisibility(memberDescriptor: MemberDescriptor): KtVisibility =
    when (memberDescriptor) {
      is MethodDescriptor -> inferredMethodKtVisibility(memberDescriptor)
      is FieldDescriptor -> KtVisibility.PUBLIC
      else -> error("$this.inferredKtVisibility")
    }

  private fun inferredMethodKtVisibility(methodDescriptor: MethodDescriptor): KtVisibility =
    when {
      methodDescriptor.isEnumConstructor -> KtVisibility.PRIVATE
      else ->
        methodDescriptor.javaOverriddenMethodDescriptors
          .map { ktVisibility(it) }
          .withWidestScopeOrNull() ?: KtVisibility.PUBLIC
    }

  /** Kotlin mangled name for this member descriptor. */
  fun ktMangledName(memberDescriptor: MemberDescriptor): String =
    if (AstUtils.isJsEnumCustomValueField(memberDescriptor)) memberDescriptor.name!!
    else memberDescriptor.ktName + ktNameSuffix(memberDescriptor)

  /** Kotlin name suffix for this member descriptor. */
  private fun ktNameSuffix(memberDescriptor: MemberDescriptor): String =
    when (memberDescriptor.visibility!!) {
      Visibility.PUBLIC -> memberDescriptor.ktPropertyNameSuffix
      Visibility.PROTECTED -> memberDescriptor.ktPropertyNameSuffix
      Visibility.PACKAGE_PRIVATE -> "_pp_${memberDescriptor.ktPackageProtectedNameSuffix}"
      Visibility.PRIVATE ->
        when (ktVisibility(memberDescriptor)) {
          KtVisibility.PRIVATE -> memberDescriptor.ktPropertyNameSuffix
          else -> "_private_${memberDescriptor.ktPrivateNameSuffix}"
        }
    }

  internal fun isKtNameMangled(memberDescriptor: MemberDescriptor): Boolean =
    memberDescriptor.name != ktMangledName(memberDescriptor)

  internal fun captureIndex(captureTypeVariable: TypeVariable): Int =
    captureIndices.computeIfAbsent(captureTypeVariable) { captureIndices.size }
}
