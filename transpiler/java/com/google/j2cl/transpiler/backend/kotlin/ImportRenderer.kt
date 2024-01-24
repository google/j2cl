/*
 * Copyright 2022 Google Inc.
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

import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.AS_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.IMPORT_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.STAR_OPERATOR
import com.google.j2cl.transpiler.backend.kotlin.ast.Import
import com.google.j2cl.transpiler.backend.kotlin.ast.Import.Companion.lexicographicalOrder
import com.google.j2cl.transpiler.backend.kotlin.ast.Import.Companion.starImport
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.join
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.orEmpty

/**
 * Kotlin import renderer.
 *
 * @property nameRenderer underlying name renderer
 */
internal data class ImportRenderer(val nameRenderer: NameRenderer) {
  /** Source with rendered imports. */
  val importsSource: Source
    get() = newLineSeparated(imports.map { it.source })

  /** A set of default imports. */
  private val defaultImports: Set<Import>
    get() = setOf(starImport("javaemul", "lang"))

  /** A list of imports to render. */
  private val imports: List<Import>
    get() =
      defaultImports.plus(nameRenderer.environment.importsSet).sortedWith(lexicographicalOrder())

  companion object {
    /** Source for this import. */
    private val Import.source: Source
      get() =
        spaceSeparated(
          IMPORT_KEYWORD,
          join(dotSeparated(pathComponents.map(::identifierSource)), suffixOrNull?.source.orEmpty()),
        )

    private val Import.Suffix.source: Source
      get() =
        when (this) {
          is Import.Suffix.WithAlias ->
            join(Source.SPACE, spaceSeparated(AS_KEYWORD, identifierSource(alias)))
          is Import.Suffix.WithStar -> join(Source.DOT, STAR_OPERATOR)
        }
  }
}
