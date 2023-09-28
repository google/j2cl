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

import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.ampersandSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.block
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.colonSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.commaAndNewLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inAngleBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inCurlyBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inDoubleQuotes
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inInlineCurlyBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inParentheses
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inParenthesesIfNotEmpty
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inSquareBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.infix
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.join
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.semicolonSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.testing.assertBuilds
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SourceTest {
  private val nullSource: Source?
    get() = null

  private val sourceA: Source
    get() = source("a")

  private val sourceB: Source
    get() = source("b")

  private val sourceC: Source
    get() = source("c")

  private val emptySources: Iterable<Source>
    get() = listOf()

  private val sourcesA: Iterable<Source>
    get() = listOf(sourceA)

  private val sourcesAB: Iterable<Source>
    get() = listOf(sourceA, sourceB)

  private val sourcesABC: Iterable<Source>
    get() = listOf(sourceA, sourceB, sourceC)

  private val sourcesABCAndEmpty: Iterable<Source>
    get() = listOf(sourceA, Source.EMPTY, sourceB, Source.EMPTY, sourceC)

  @Test
  fun emptySourceIsEmpty() {
    Source.EMPTY.assertBuilds("")
  }

  @Test
  fun stringSource() {
    source("foo").assertBuilds("foo")
  }

  @Test
  fun sourcePlusSource() {
    (sourceA + sourceB).assertBuilds("ab")
  }

  @Test
  fun sourceJoin() {
    join().assertBuilds("")
    join(sourceA).assertBuilds("a")
    join(sourceA, sourceB).assertBuilds("ab")
    join(sourceA, Source.EMPTY, sourceB).assertBuilds("ab")
  }

  @Test
  fun sourceJoin_withSeparator() {
    join(emptySources, separator = ", ").assertBuilds("")
    join(sourcesA, separator = ", ").assertBuilds("a")
    join(sourcesAB, separator = ", ").assertBuilds("a, b")
    join(sourcesABC, separator = ", ").assertBuilds("a, b, c")
    join(sourcesABCAndEmpty, separator = ", ").assertBuilds("a, b, c")
  }

  @Test
  fun separatedSources_vararg() {
    spaceSeparated(sourceA, Source.EMPTY, sourceB, Source.EMPTY, sourceC).assertBuilds("a b c")
    commaSeparated(sourceA, Source.EMPTY, sourceB, Source.EMPTY, sourceC).assertBuilds("a, b, c")
    dotSeparated(sourceA, Source.EMPTY, sourceB, Source.EMPTY, sourceC).assertBuilds("a.b.c")
    colonSeparated(sourceA, Source.EMPTY, sourceB, Source.EMPTY, sourceC).assertBuilds("a: b: c")
    newLineSeparated(sourceA, Source.EMPTY, sourceB, Source.EMPTY, sourceC).assertBuilds("a\nb\nc")
    emptyLineSeparated(sourceA, Source.EMPTY, sourceB, Source.EMPTY, sourceC)
      .assertBuilds("a\n\nb\n\nc")
  }

  @Test
  fun separatedSources_iterable() {
    spaceSeparated(sourcesABCAndEmpty).assertBuilds("a b c")
    commaSeparated(sourcesABCAndEmpty).assertBuilds("a, b, c")
    commaAndNewLineSeparated(sourcesABCAndEmpty).assertBuilds("a,\nb,\nc")
    dotSeparated(sourcesABCAndEmpty).assertBuilds("a.b.c")
    ampersandSeparated(sourcesABCAndEmpty).assertBuilds("a & b & c")
    colonSeparated(sourcesABCAndEmpty).assertBuilds("a: b: c")
    semicolonSeparated(sourcesABCAndEmpty).assertBuilds("a; b; c")
    newLineSeparated(sourcesABCAndEmpty).assertBuilds("a\nb\nc")
    emptyLineSeparated(sourcesABCAndEmpty).assertBuilds("a\n\nb\n\nc")
  }

  @Test
  fun sourceInCurlyBrackets() {
    inCurlyBrackets(source("")).assertBuilds("{}")
    inCurlyBrackets(source("\n")).assertBuilds("{\n \n}")
    inCurlyBrackets(source("\nfoo")).assertBuilds("{\n foo\n}")
    inCurlyBrackets(source("\nfoo\nbar")).assertBuilds("{\n foo\n bar\n}")
    inCurlyBrackets(source("foo\nbar")).assertBuilds("{foo\n bar\n}")
  }

  @Test
  fun sourceInBrackets() {
    inParentheses(sourceA).assertBuilds("(a)")
    inAngleBrackets(sourceA).assertBuilds("<a>")
    inSquareBrackets(sourceA).assertBuilds("[a]")
    inDoubleQuotes(sourceA).assertBuilds("\"a\"")
    inInlineCurlyBrackets(sourceA).assertBuilds("{ a }")

    inParenthesesIfNotEmpty(Source.EMPTY).assertBuilds("")
    inParenthesesIfNotEmpty(sourceA).assertBuilds("(a)")
  }

  @Test
  fun blockSource() {
    block(Source.EMPTY).assertBuilds("{}")
    block(sourceA).assertBuilds("{\n a\n}")
    block(newLineSeparated(sourceA, sourceB)).assertBuilds("{\n a\n b\n}")
  }

  @Test
  fun infixSource() {
    infix(sourceA, "op", sourceB).assertBuilds("a op b")
    infix(Source.EMPTY, "op", Source.EMPTY).assertBuilds("op")
  }

  @Test
  fun sourceIf() {
    Source.emptyUnless(true) { sourceA }.assertBuilds("a")
    Source.emptyUnless(false) { sourceA }.assertBuilds("")
  }

  @Test
  fun sourceIfEmpty() {
    sourceA.ifEmpty { sourceB }.assertBuilds("a")
    Source.EMPTY.ifEmpty { sourceB }.assertBuilds("b")
  }

  @Test
  fun sourceIfNotEmpty() {
    sourceA.ifNotEmpty { inParentheses(it) }.assertBuilds("(a)")
    Source.EMPTY.ifNotEmpty { inParentheses(it) }.assertBuilds("")
  }

  @Test
  fun sourceOrEmpty() {
    sourceA.orEmpty().assertBuilds("a")
    nullSource.orEmpty().assertBuilds("")
  }
}
