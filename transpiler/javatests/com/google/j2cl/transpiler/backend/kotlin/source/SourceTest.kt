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

import com.google.common.truth.Truth.assertThat
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

  private infix fun Source.gives(string: String) {
    assertThat(buildString()).isEqualTo(string)
  }

  @Test
  fun emptySourceIsEmpty() {
    Source.EMPTY gives ""
  }

  @Test
  fun stringSource() {
    source("foo") gives "foo"
  }

  @Test
  fun sourcePlusSource() {
    sourceA + sourceB gives "ab"
  }

  @Test
  fun sourceJoin() {
    join() gives ""
    join(sourceA) gives "a"
    join(sourceA, sourceB) gives "ab"
    join(sourceA, Source.EMPTY, sourceB) gives "ab"
  }

  @Test
  fun sourceJoin_withSeparator() {
    join(emptySources, separator = ", ") gives ""
    join(sourcesA, separator = ", ") gives "a"
    join(sourcesAB, separator = ", ") gives "a, b"
    join(sourcesABC, separator = ", ") gives "a, b, c"
    join(sourcesABCAndEmpty, separator = ", ") gives "a, b, c"
  }

  @Test
  fun separatedSources_vararg() {
    spaceSeparated(sourceA, Source.EMPTY, sourceB, Source.EMPTY, sourceC) gives "a b c"
    commaSeparated(sourceA, Source.EMPTY, sourceB, Source.EMPTY, sourceC) gives "a, b, c"
    dotSeparated(sourceA, Source.EMPTY, sourceB, Source.EMPTY, sourceC) gives "a.b.c"
    colonSeparated(sourceA, Source.EMPTY, sourceB, Source.EMPTY, sourceC) gives "a: b: c"
    newLineSeparated(sourceA, Source.EMPTY, sourceB, Source.EMPTY, sourceC) gives "a\nb\nc"
    emptyLineSeparated(sourceA, Source.EMPTY, sourceB, Source.EMPTY, sourceC) gives "a\n\nb\n\nc"
  }

  @Test
  fun separatedSources_iterable() {
    spaceSeparated(sourcesABCAndEmpty) gives "a b c"
    commaSeparated(sourcesABCAndEmpty) gives "a, b, c"
    commaAndNewLineSeparated(sourcesABCAndEmpty) gives "a,\nb,\nc"
    dotSeparated(sourcesABCAndEmpty) gives "a.b.c"
    ampersandSeparated(sourcesABCAndEmpty) gives "a & b & c"
    colonSeparated(sourcesABCAndEmpty) gives "a: b: c"
    semicolonSeparated(sourcesABCAndEmpty) gives "a; b; c"
    newLineSeparated(sourcesABCAndEmpty) gives "a\nb\nc"
    emptyLineSeparated(sourcesABCAndEmpty) gives "a\n\nb\n\nc"
  }

  @Test
  fun sourceInCurlyBrackets() {
    inCurlyBrackets(source("")) gives "{}"
    inCurlyBrackets(source("\n")) gives "{\n \n}"
    inCurlyBrackets(source("\nfoo")) gives "{\n foo\n}"
    inCurlyBrackets(source("\nfoo\nbar")) gives "{\n foo\n bar\n}"
    inCurlyBrackets(source("foo\nbar")) gives "{foo\n bar\n}"
  }

  @Test
  fun sourceInBrackets() {
    inParentheses(sourceA) gives "(a)"
    inAngleBrackets(sourceA) gives "<a>"
    inSquareBrackets(sourceA) gives "[a]"
    inDoubleQuotes(sourceA) gives "\"a\""
    inInlineCurlyBrackets(sourceA) gives "{ a }"

    inParenthesesIfNotEmpty(Source.EMPTY) gives ""
    inParenthesesIfNotEmpty(sourceA) gives "(a)"
  }

  @Test
  fun blockSource() {
    block(Source.EMPTY) gives "{}"
    block(sourceA) gives "{\n a\n}"
    block(newLineSeparated(sourceA, sourceB)) gives "{\n a\n b\n}"
  }

  @Test
  fun infixSource() {
    infix(sourceA, "op", sourceB) gives "a op b"
    infix(Source.EMPTY, "op", Source.EMPTY) gives "op"
  }

  @Test
  fun sourceIf() {
    Source.emptyUnless(true) { sourceA } gives "a"
    Source.emptyUnless(false) { sourceA } gives ""
  }

  @Test
  fun sourceIfEmpty() {
    sourceA.ifEmpty { sourceB } gives "a"
    Source.EMPTY.ifEmpty { sourceB } gives "b"
  }

  @Test
  fun sourceIfNotEmpty() {
    sourceA.ifNotEmpty { inParentheses(it) } gives "(a)"
    Source.EMPTY.ifNotEmpty { inParentheses(it) } gives ""
  }

  @Test
  fun sourceOrEmpty() {
    sourceA.orEmpty() gives "a"
    nullSource.orEmpty() gives ""
  }
}
