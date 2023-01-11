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
package com.google.j2cl.transpiler.backend.kotlin.source

import com.google.j2cl.transpiler.backend.common.SourceBuilder

/** A piece of source which can be appended to SourceBuilder. */
data class Source(val appendTo: (SourceBuilder) -> Unit) {
  override fun toString(): String = SourceBuilder().also { it.append(this) }.build()
}

fun SourceBuilder.append(source: Source) = source.appendTo(this)

fun source(string: String) = Source { it.append(string) }

operator fun Source.plus(source: Source) = Source {
  appendTo(it)
  source.appendTo(it)
}

val Source.plusSemicolon
  get() = this + source(";")

val Source.plusNewLine
  get() = this + source("\n")

fun inNewLine(source: Source) = source("\n") + source

fun inRoundBrackets(source: Source) = join(source("("), source, source(")"))

fun inAngleBrackets(source: Source) = join(source("<"), source, source(">"))

fun inCurlyBrackets(source: Source) = Source {
  it.openBrace()
  it.append(source)
  it.closeBrace()
}

fun join(sources: Iterable<Source>) = Source { sources.forEach<Source>(it::append) }

infix fun String.separated(sources: Iterable<Source>): Source = Source {
  var first = true
  for (source in sources) {
    if (first) first = false else it.append(this)
    it.append(source)
  }
}

fun spaceSeparated(sources: Iterable<Source>) = " " separated sources

fun commaSeparated(sources: Iterable<Source>) = ", " separated sources

fun newLineSeparated(sources: Iterable<Source>) = "\n" separated sources

fun emptyLineSeparated(sources: Iterable<Source>) = "\n\n" separated sources

fun inNewLines(sources: Iterable<Source>) = join(sources.map(::inNewLine))

fun join(source: Source?, vararg sources: Source?) = join(listOfNotNull(source, *sources))

fun spaceSeparated(source: Source?, vararg sources: Source?) =
  spaceSeparated(listOfNotNull(source, *sources))

fun commaSeparated(source: Source?, vararg sources: Source?) =
  commaSeparated(listOfNotNull(source, *sources))

fun emptyLineSeparated(source: Source?, vararg sources: Source?) =
  emptyLineSeparated(listOfNotNull(source, *sources))

fun infix(lhs: Source, operator: String, rhs: Source) = spaceSeparated(lhs, source(operator), rhs)
