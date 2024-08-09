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

import com.google.j2cl.common.OutputUtils
import com.google.j2cl.common.Problems
import com.google.j2cl.transpiler.ast.AbstractVisitor
import com.google.j2cl.transpiler.ast.CompilationUnit
import com.google.j2cl.transpiler.ast.FunctionExpression
import com.google.j2cl.transpiler.ast.HasName
import com.google.j2cl.transpiler.ast.Library
import com.google.j2cl.transpiler.ast.MemberDescriptor
import com.google.j2cl.transpiler.ast.MemberReference
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.backend.common.UniqueNamesResolver.computeUniqueNames
import com.google.j2cl.transpiler.backend.kotlin.source.Source

/**
 * The OutputGeneratorStage contains all necessary information for generating the Kotlin output for
 * the transpiler. It is responsible for generating implementation files for each Java file.
 *
 * @property output output for generated sources
 * @property problems problems collected during generation
 */
class KotlinGeneratorStage(private val output: OutputUtils.Output, private val problems: Problems) {
  /** Generate outputs for a library. */
  fun generateOutputs(library: Library) {
    library.compilationUnits.forEach { generateOutputs(it) }
  }

  /** Generate all outputs for a compilation unit. */
  private fun generateOutputs(compilationUnit: CompilationUnit) {
    generateKtOutputs(compilationUnit)
    generateObjCOutputs(compilationUnit)
  }

  /** Generate Kotlin outputs for a compilation unit. */
  private fun generateKtOutputs(compilationUnit: CompilationUnit) {
    val source = ktSource(compilationUnit).buildString().trimTrailingWhitespaces()
    val path = compilationUnit.packageRelativePath.replace(".java", ".kt")
    output.write(path, source)
  }

  /** Generate ObjC outputs for a compilation unit. */
  private fun generateObjCOutputs(compilationUnit: CompilationUnit) {
    val source = compilationUnit.j2ObjCCompatHeaderSource
    if (!source.isEmpty()) {
      val path = compilationUnit.packageRelativePath.replace(".java", "+J2ObjCCompat.h")
      output.write(path, source.buildString())
    }
  }

  /** Returns Kotlin source for a compilation unit. */
  private fun ktSource(compilationUnit: CompilationUnit): Source {
    val nameToIdentifierMap = compilationUnit.buildNameToIdentifierMap()

    val environment =
      Environment(
        nameToIdentifierMap = nameToIdentifierMap,
        identifierSet = nameToIdentifierMap.values.toSet(),
        privateAsKtInternalDeclarationMemberDescriptorSet =
          compilationUnit.buildPrivateKtInternalMemberDescriptorSet(),
      )

    val nameRenderer =
      NameRenderer(environment).plusLocalTypeNameMap(compilationUnit.localTypeNames)

    val compilationUnitRenderer = CompilationUnitRenderer(nameRenderer)

    return compilationUnitRenderer.source(compilationUnit)
  }
}

/** Returns string with trimmed trailing whitespaces. */
private fun String.trimTrailingWhitespaces() = lines().joinToString("\n") { it.trimEnd() }

/** Returns a map from all named nodes in this compilation unit to rendered identifier strings. */
private fun CompilationUnit.buildNameToIdentifierMap(): Map<HasName, String> = buildMap {
  buildForbiddenIdentifierSet().let { forbiddenIdentifiers ->
    streamTypes().forEach { type -> putAll(computeUniqueNames(forbiddenIdentifiers, type)) }
  }
}

/** Returns a set with forbidden identifier strings in this compilation unit. */
private fun CompilationUnit.buildForbiddenIdentifierSet(): Set<String> = buildSet {
  accept(
    object : AbstractVisitor() {
      override fun enterFunctionExpression(functionExpression: FunctionExpression): Boolean {
        // Functional interface names are forbidden because they are rendered in return statement
        // labels.
        add(functionExpression.typeDescriptor.functionalInterface!!.typeDeclaration.ktSimpleName)
        return true
      }
    }
  )
}

/**
 * Build a set of private member descriptors in this compilation unit which should be rendered as
 * internal in Kotlin.
 */
private fun CompilationUnit.buildPrivateKtInternalMemberDescriptorSet(): Set<MemberDescriptor> =
  buildSet {
    accept(
      object : AbstractVisitor() {
        override fun exitType(type: Type) {
          // If the type is not enclosed inside its super-type, convert all private constructors in
          // a super-type to internal to make them accessible from this type.
          // TODO(b/352547776): Let it be driven by the presence of super-calls.
          val superTypeDeclaration = type.declaration.superTypeDeclaration
          val currentTypeDeclaration = currentType.declaration
          if (
            superTypeDeclaration != null &&
              !currentTypeDeclaration.equalsOrEnclosedIn(superTypeDeclaration)
          ) {
            superTypeDeclaration.declaredMethodDescriptors
              .asSequence()
              .filter { it.isConstructor && it.visibility.isPrivate }
              .forEach { add(it) }
          }
        }

        override fun exitMemberReference(memberReference: MemberReference) {
          // Add declared member if it's referenced outside its enclosing type.
          val memberDescriptor = memberReference.target.declarationDescriptor
          val currentTypeDeclaration = currentType.declaration
          if (memberDescriptor.visibility.isPrivate) {
            val enclosingTypeDeclaration = memberDescriptor.enclosingTypeDescriptor.typeDeclaration
            if (!currentTypeDeclaration.equalsOrEnclosedIn(enclosingTypeDeclaration)) {
              add(memberDescriptor)
            }
          }
        }
      }
    )
  }
