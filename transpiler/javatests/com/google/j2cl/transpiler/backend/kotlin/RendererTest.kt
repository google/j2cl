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

import com.google.j2cl.common.Problems
import com.google.j2cl.transpiler.backend.common.SourceBuilder
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RendererTest {
  @Test
  fun string() {
    assertEquals("foo", renderedString { render("foo") })
  }

  @Test
  fun newLine() {
    assertEquals("\n", renderedString { renderNewLine() })
  }

  @Test
  fun indentation() {
    assertEquals(
      "foo\n bar\n zar\ngoo",
      renderedString {
        render("foo")
        renderIndented {
          renderNewLine()
          render("bar\nzar")
        }
        renderNewLine()
        render("goo")
      }
    )
  }

  @Test
  fun parentheses() {
    assertEquals("(foo)", renderedString { renderInParentheses { render("foo") } })
  }

  @Test
  fun angleBrackets() {
    assertEquals("<foo>", renderedString { renderInAngleBrackets { render("foo") } })
  }

  @Test
  fun curlyBrackets() {
    assertEquals("{foo\n}", renderedString { renderInCurlyBrackets { render("foo") } })
  }

  @Test
  fun squareBrackets() {
    assertEquals("[foo]", renderedString { renderInSquareBrackets { render("foo") } })
  }

  @Test
  fun commaSeparated_empty() {
    assertEquals("", renderedString { renderCommaSeparated(listOf<String>()) { render(it) } })
  }

  @Test
  fun commaSeparated_single() {
    assertEquals("foo", renderedString { renderCommaSeparated(listOf("foo")) { render(it) } })
  }

  @Test
  fun commaSeparated_multiple() {
    assertEquals(
      "foo, bar, goo",
      renderedString { renderCommaSeparated(listOf("foo", "bar", "goo")) { render(it) } }
    )
  }

  @Test
  fun startingWithNewLines_empty() {
    assertEquals("", renderedString { renderStartingWithNewLines(listOf<String>()) { render(it) } })
  }

  @Test
  fun startingWithNewLines_single() {
    assertEquals(
      "\nfoo",
      renderedString { renderStartingWithNewLines(listOf("foo")) { render(it) } }
    )
  }

  @Test
  fun startingWithNewLines_multiple() {
    assertEquals(
      "\nfoo\nbar\ngoo",
      renderedString { renderStartingWithNewLines(listOf("foo", "bar", "goo")) { render(it) } }
    )
  }

  private fun renderedString(fn: Renderer.() -> Unit): String =
    Renderer(Environment(), SourceBuilder(), Problems()).apply(fn).sourceBuilder.build()
}
