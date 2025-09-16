/*
 * Copyright 2023 Google Inc.
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
package com.google.j2cl.transpiler.backend.kotlin.objc

import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inAngleBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inDoubleQuotes
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.join
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated

fun comment(source: Source): Source = spaceSeparated(source("//"), source)

fun Source.plusSemicolon(): Source = plus(Source.SEMICOLON)

fun macroDeclaration(source: Source) = join(source("#"), source)

fun macroDefine(source: Source) = macroDeclaration(spaceSeparated(source("define"), source))

fun compatibilityAlias(alias: Source, target: Source) =
  spaceSeparated(source("@compatibility_alias"), alias, target)

fun defineAlias(alias: Source, target: Source) = macroDefine(spaceSeparated(alias, target))

fun dependenciesSource(dependencies: Iterable<Dependency>): Source =
  emptyLineSeparated(
    importsSource(dependencies.imports()),
    forwardDeclarationsSource(dependencies.forwardDeclarations()),
  )

fun importsSource(imports: List<Import>): Source =
  emptyLineSeparated(
    newLineSeparated(imports.filter { !it.isLocal }.sortedBy { it.path }.map { it.source }),
    newLineSeparated(imports.filter { it.isLocal }.sortedBy { it.path }.map { it.source }),
  )

fun forwardDeclarationsSource(forwardDeclarations: List<ForwardDeclaration>): Source =
  emptyLineSeparated(
    forwardDeclarations
      .groupBy { it.kind }
      .entries
      .sortedBy { it.key }
      .map { it.value.sortedBy { it.name } }
      .map { newLineSeparated(it.map { it.source }) }
  )

val ForwardDeclaration.source: Source
  get() = spaceSeparated(kind.source, source(name)).plusSemicolon()

val ForwardDeclaration.Kind.source: Source
  get() =
    source(
      when (this) {
        ForwardDeclaration.Kind.CLASS -> "@class"
        ForwardDeclaration.Kind.PROTOCOL -> "@protocol"
      }
    )

val Import.source: Source
  get() =
    spaceSeparated(
      source("#import"),
      source(path).let {
        if (isLocal) {
          inDoubleQuotes(it)
        } else {
          inAngleBrackets(it)
        }
      },
    )

val Renderer<Source>.sourceWithDependencies: Source
  get() =
    renderWithDependencies().let { (source, dependencies) ->
      emptyLineSeparated(dependenciesSource(dependencies), source)
    }
