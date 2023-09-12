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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SourceTest {
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
    get() = listOf(sourceA, emptySource, sourceB, emptySource, sourceC)

  private infix fun Source.gives(string: String) {
    assertThat(toString()).isEqualTo(string)
  }

  @Test
  fun emptySource() {
    emptySource gives ""
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
  fun joinSources() {
    join(sourceA) gives "a"
    join(sourceA, sourceB) gives "ab"
    join(sourceA, emptySource, sourceB) gives "ab"
  }

  @Test
  fun stringSeparatedSources() {
    ", " separated emptySources gives ""
    ", " separated sourcesA gives "a"
    ", " separated sourcesAB gives "a, b"
    ", " separated sourcesABC gives "a, b, c"
    ", " separated sourcesABCAndEmpty gives "a, b, c"
  }

  @Test
  fun separatedSources_vararg() {
    spaceSeparated(sourceA, emptySource, sourceB, emptySource, sourceC) gives "a b c"
    commaSeparated(sourceA, emptySource, sourceB, emptySource, sourceC) gives "a, b, c"
    dotSeparated(sourceA, emptySource, sourceB, emptySource, sourceC) gives "a.b.c"
    colonSeparated(sourceA, emptySource, sourceB, emptySource, sourceC) gives "a: b: c"
    newLineSeparated(sourceA, emptySource, sourceB, emptySource, sourceC) gives "a\nb\nc"
    emptyLineSeparated(sourceA, emptySource, sourceB, emptySource, sourceC) gives "a\n\nb\n\nc"
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
  fun bracketedSources() {
    inParentheses(sourceA) gives "(a)"
    inAngleBrackets(sourceA) gives "<a>"
    inSquareBrackets(sourceA) gives "[a]"
    inDoubleQuotes(sourceA) gives "\"a\""
    inInlineCurlyBrackets(sourceA) gives "{ a }"

    inParenthesesIfNotEmpty(emptySource) gives ""
    inParenthesesIfNotEmpty(sourceA) gives "(a)"
  }

  @Test
  fun blockSource() {
    block(emptySource) gives "{}"
    block(sourceA) gives "{\n a\n}"
    block(newLineSeparated(sourceA, sourceB)) gives "{\n a\n b\n}"
  }

  @Test
  fun infixSource() {
    infix(sourceA, "op", sourceB) gives "a op b"
  }

  @Test
  fun sourceIf() {
    sourceIf(true) { sourceA } gives "a"
    sourceIf(false) { sourceA } gives ""
  }

  @Test
  fun sourceIfEmpty() {
    sourceA.ifEmpty { sourceB } gives "a"
    emptySource.ifEmpty { sourceB } gives "b"
  }

  @Test
  fun sourceIfNotEmpty() {
    sourceA.ifNotEmpty(::inParentheses) gives "(a)"
    emptySource.ifNotEmpty(::inParentheses) gives ""
  }

  @Test
  fun ifNotNullSource() {
    128.ifNotNullSource { source(it.inc().toString()) } gives "129"
    (null as Int?).ifNotNullSource { source(it.inc().toString()) } gives ""
  }
}
