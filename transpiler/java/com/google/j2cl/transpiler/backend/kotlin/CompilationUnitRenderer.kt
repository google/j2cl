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

import com.google.j2cl.transpiler.ast.CompilationUnit
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.PACKAGE_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.fileAnnotation
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.literal
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.orEmpty

/**
 * Compilation unit renderer.
 *
 * @param nameRenderer the underlying name renderer
 */
internal data class CompilationUnitRenderer(val nameRenderer: NameRenderer) {

  /** Returns source for the given compilation unit. */
  fun source(compilationUnit: CompilationUnit): Source {
    // Render types, collecting qualified names to import
    val typesSource = typesSource(compilationUnit)

    // Render file header, collecting qualified names to import
    val fileHeaderSource = fileHeaderSource(compilationUnit)

    // Render package and collected imports
    val packageAndImportsSource = packageAndImportsSource(compilationUnit)

    val completeSource = emptyLineSeparated(fileHeaderSource, packageAndImportsSource, typesSource)

    return completeSource.plus(Source.NEW_LINE)
  }

  private val importRenderer: ImportRenderer
    get() = ImportRenderer(nameRenderer)

  private fun fileHeaderSource(compilationUnit: CompilationUnit): Source =
    newLineSeparated(fileCommentSource(compilationUnit), fileAnnotationsSource())

  private fun packageAndImportsSource(compilationUnit: CompilationUnit): Source =
    emptyLineSeparated(packageSource(compilationUnit), importRenderer.importsSource)

  private fun typesSource(compilationUnit: CompilationUnit): Source =
    emptyLineSeparated(compilationUnit.types.map(::typeSource))

  private fun fileCommentSource(compilationUnit: CompilationUnit) =
    source("// Generated from \"${compilationUnit.packageRelativePath}\"")

  private fun fileAnnotationsSource(): Source =
    newLineSeparated(fileOptInAnnotationSource, suppressFileAnnotationsSource)

  private fun fileOptInAnnotationSource(features: List<Source>): Source =
    fileAnnotation(nameRenderer.topLevelQualifiedNameSource("kotlin.OptIn"), features)

  private val fileOptInAnnotationSource: Source
    get() =
      nameRenderer.environment.importedOptInQualifiedNamesSet
        .takeIf { it.isNotEmpty() }
        ?.map { KotlinSource.classLiteral(nameRenderer.topLevelQualifiedNameSource(it)) }
        ?.let { fileOptInAnnotationSource(it) }
        .orEmpty()

  private val suppressFileAnnotationsSource: Source
    get() =
      fileAnnotation(
        nameRenderer.topLevelQualifiedNameSource("kotlin.Suppress"),
        listOf(
            "ALWAYS_NULL",
            "PARAMETER_NAME_CHANGED_ON_OVERRIDE",
            "REPEATED_BOUND",
            "SENSELESS_COMPARISON",
            "UNCHECKED_CAST",
            "UNNECESSARY_LATEINIT",
            "UNNECESSARY_NOT_NULL_ASSERTION",
            "UNREACHABLE_CODE",
            "UNUSED_ANONYMOUS_PARAMETER",
            "UNUSED_PARAMETER",
            "UNUSED_VARIABLE",
            "USELESS_CAST",
            "VARIABLE_IN_SINGLETON_WITHOUT_THREAD_LOCAL",
            "VARIABLE_WITH_REDUNDANT_INITIALIZER",
          )
          .map { literal(it) },
      )

  private fun packageSource(compilationUnit: CompilationUnit): Source =
    compilationUnit.packageName
      .takeIf { it.isNotEmpty() }
      ?.let { spaceSeparated(PACKAGE_KEYWORD, qualifiedIdentifierSource(it)) }
      .orEmpty()

  private fun typeSource(type: Type): Source = TypeRenderer(nameRenderer).typeSource(type)
}
