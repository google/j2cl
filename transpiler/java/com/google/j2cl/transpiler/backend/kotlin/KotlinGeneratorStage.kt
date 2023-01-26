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
import com.google.j2cl.transpiler.backend.common.UniqueNamesResolver.computeUniqueNames
import com.google.j2cl.transpiler.backend.kotlin.common.buildMap
import com.google.j2cl.transpiler.backend.kotlin.common.buildSet
import com.google.j2cl.transpiler.backend.kotlin.source.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.plusNewLine

/**
 * The OutputGeneratorStage contains all necessary information for generating the Kotlin output for
 * the transpiler. It is responsible for generating implementation files for each Java file.
 */
class KotlinGeneratorStage(private val output: OutputUtils.Output, private val problems: Problems) {
  fun generateOutputs(library: Library) {
    library.compilationUnits.forEach { generateOutputs(it) }
  }

  private fun generateOutputs(compilationUnit: CompilationUnit) {
    generateKtOutputs(compilationUnit)
    generateObjCOutputs(compilationUnit)
  }

  private fun generateKtOutputs(compilationUnit: CompilationUnit) {
    val source = ktSource(compilationUnit)
    val path = compilationUnit.packageRelativePath.replace(".java", ".kt")
    output.write(path, source)
  }

  private fun generateObjCOutputs(compilationUnit: CompilationUnit) {
    val source = compilationUnit.j2ObjCCompatHeaderSource
    if (!source.isEmpty) {
      val path = compilationUnit.packageRelativePath.replace(".java", "+J2ObjCCompat.h")
      output.write(path, source.toString())
    }
  }

  private fun ktSource(compilationUnit: CompilationUnit): String {
    val nameToIdentifierMap = compilationUnit.buildNameToIdentifierMap()

    val environment =
      Environment(
        nameToIdentifierMap = nameToIdentifierMap,
        identifierSet = nameToIdentifierMap.values.toSet()
      )

    val renderer =
      Renderer(
        environment,
        problems,
        topLevelQualifiedNames = compilationUnit.topLevelQualifiedNames
      )

    // Render types, collecting qualified names to import
    val typesSource = renderer.typesSource(compilationUnit)

    // Render file header, collecting qualified names to import
    val fileHeaderSource = renderer.fileHeaderSource(compilationUnit)

    // Render package and collected imports
    val packageAndImportsSource = renderer.packageAndImportsSource(compilationUnit)

    val completeSource = emptyLineSeparated(fileHeaderSource, packageAndImportsSource, typesSource)

    return completeSource.plusNewLine.toString().trimTrailingWhitespaces()
  }
}

private fun String.trimTrailingWhitespaces() = lines().joinToString("\n") { it.trimEnd() }

private fun CompilationUnit.buildNameToIdentifierMap(): Map<HasName, String> = buildMap {
  buildForbiddenNamesSet().let { forbiddenNames ->
    streamTypes().forEach { type -> putAll(computeUniqueNames(forbiddenNames, type)) }
  }
}

private fun CompilationUnit.buildForbiddenNamesSet(): Set<String> = buildSet {
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
