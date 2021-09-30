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

/** Renderer of the Kotlin source code. */
class Renderer(
  /** Rendering environment. */
  val environment: Environment,

  /** Output source builder. */
  val sourceBuilder: SourceBuilder,

  /** Rendering problems. */
  val problems: Problems
) {
  fun renderNewLine() {
    sourceBuilder.newLine()
  }

  fun render(string: String) {
    sourceBuilder.append(string)
  }

  fun renderIndented(renderFn: () -> Unit) {
    sourceBuilder.indent()
    renderFn()
    sourceBuilder.unindent()
  }

  fun renderInCurlyBrackets(renderFn: () -> Unit) {
    sourceBuilder.openBrace()
    renderFn()
    sourceBuilder.closeBrace()
  }

  fun renderInParentheses(renderFn: () -> Unit) {
    render("(")
    renderFn()
    render(")")
  }

  fun renderInSquareBrackets(renderFn: () -> Unit) {
    render("[")
    renderFn()
    render("]")
  }

  fun renderInAngleBrackets(renderFn: () -> Unit) {
    render("<")
    renderFn()
    render(">")
  }

  fun <V> renderSeparatedWith(values: Iterable<V>, separator: String, renderFn: (V) -> Unit) {
    var first = true
    for (value in values) {
      if (first) {
        first = false
      } else {
        render(separator)
      }
      renderFn(value)
    }
  }

  fun <V> renderCommaSeparated(values: Iterable<V>, renderFn: (V) -> Unit) {
    renderSeparatedWith(values, ", ", renderFn)
  }

  fun <V> renderStartingWithNewLines(values: Iterable<V>, renderFn: (V) -> Unit) {
    for (value in values) {
      renderNewLine()
      renderFn(value)
    }
  }

  fun <V> renderSeparatedWithEmptyLine(values: Iterable<V>, renderFn: (V) -> Unit) {
    renderSeparatedWith(values, "\n\n", renderFn)
  }
}
