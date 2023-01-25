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

import com.google.j2cl.transpiler.backend.kotlin.ast.Import
import com.google.j2cl.transpiler.backend.kotlin.ast.defaultImports
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.infix
import com.google.j2cl.transpiler.backend.kotlin.source.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.source
import com.google.j2cl.transpiler.backend.kotlin.source.spaceSeparated

private val Renderer.imports: List<Import>
  get() =
    defaultImports
      .plus(
        environment.importedSimpleNameToQualifiedNameMap.entries.map { (simpleName, qualifiedName)
          ->
          Import(
            qualifiedName.qualifiedNameComponents(),
            if (qualifiedName.qualifiedNameToSimpleName() == simpleName) null
            else Import.Suffix.Alias(simpleName)
          )
        }
      )
      .sorted()

internal fun Renderer.importsSource(): Source = newLineSeparated(imports.map { source(it) })

private fun source(import: Import): Source =
  spaceSeparated(
    source("import"),
    dotSeparated(import.components.map(::identifierSource)).plus(import.suffixOrNull)
  )

private fun Source.plus(suffix: Import.Suffix?): Source =
  when (suffix) {
    is Import.Suffix.Alias -> infix(this, "as", identifierSource(suffix.alias))
    is Import.Suffix.Star -> dotSeparated(this, source("*"))
    null -> this
  }
