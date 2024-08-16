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

import com.google.j2cl.common.SourcePosition
import com.google.j2cl.transpiler.ast.MemberDescriptor
import com.google.j2cl.transpiler.backend.common.SourceBuilder

/** Composable piece of source code. */
class Source
private constructor(
  /**
   * A function which appends a non-empty content of this source to the given source builder, or
   * null to indicate that the source is empty.
   */
  private val nonEmptyAppendFn: ((SourceBuilder) -> Unit)?
) {
  /** Returns string with a content of this source. */
  fun buildString(): String = SourceBuilder().also { it.append(this) }.build()

  /** Returns whether this source is empty. */
  fun isEmpty(): Boolean = nonEmptyAppendFn == null

  /** Returns whether this source is not empty. */
  fun isNotEmpty(): Boolean = !isEmpty()

  /**
   * Returns source containing the content of this source concatenated with the content of the given
   * source.
   */
  operator fun plus(source: Source) =
    emptyUnless(isNotEmpty() || source.isNotEmpty()) {
      Source { sourceBuilder ->
        sourceBuilder.append(this)
        sourceBuilder.append(source)
      }
    }

  /** Returns this source if it's not empty, or the result of the given function otherwise. */
  inline fun ifEmpty(fn: () -> Source) = if (isEmpty()) fn() else this

  /**
   * Returns the result of the given function applied to this source if it's not empty, otherwise
   * returns this (empty) source.
   */
  inline fun ifNotEmpty(fn: (Source) -> Source) = if (isEmpty()) this else fn(this)

  /** Returns source with additional source position information. */
  fun withMapping(sourcePosition: SourcePosition): Source = withMapping { emitter ->
    emitWithMapping(sourcePosition, emitter)
  }

  /** Returns source with additional source position information for the given member. */
  fun withMapping(memberDescriptor: MemberDescriptor): Source = withMapping { emmiter ->
    emitWithMemberMapping(memberDescriptor, emmiter)
  }

  private fun withMapping(emitFn: SourceBuilder.(() -> Unit) -> Unit): Source =
    nonEmptyAppendFn?.let { appendFn ->
      Source { sourceBuilder -> emitFn(sourceBuilder) { appendFn(sourceBuilder) } }
    } ?: Source(null)

  companion object {
    val EMPTY = Source(null)
    val COMMA = source(",")
    val COLON = source(":")
    val DOT = source(".")
    val NEW_LINE = source("\n")
    val SEMICOLON = source(";")
    val SPACE = source(" ")
    val DOUBLE_QUOTE = source("\"")
    val LEFT_PARENTHESIS = source("(")
    val RIGHT_PARENTHESIS = source(")")
    val LEFT_ANGLE_BRACKET = source("<")
    val RIGHT_ANGLE_BRACKET = source(">")
    val LEFT_CURLY_BRACKET = source("{")
    val RIGHT_CURLY_BRACKET = source("}")
    val LEFT_SQUARE_BRACKET = source("[")
    val RIGHT_SQUARE_BRACKET = source("]")
    val NUMBER_SIGN = source("#")
    val HYPHEN_MINUS = source("-")

    /** Returns a source containing the given string. */
    fun source(string: String) =
      emptyUnless(string.isNotEmpty()) { Source { sourceBuilder -> sourceBuilder.append(string) } }

    /**
     * Returns source returned from the given function if the condition is satisfied, otherwise
     * returns empty source.
     */
    inline fun emptyUnless(condition: Boolean, fn: () -> Source) = if (condition) fn() else EMPTY

    /** Join given sources using given separator, skipping empty ones. */
    fun join(sources: Iterable<Source>, separator: String = "") =
      emptyUnless(sources.any(Source::isNotEmpty)) {
        Source { sourceBuilder ->
          var first = true
          for (source in sources) {
            if (source.isNotEmpty()) {
              if (first) {
                first = false
              } else {
                sourceBuilder.append(separator)
              }
              sourceBuilder.append(source)
            }
          }
        }
      }

    /** Join given sources. */
    fun join(vararg sources: Source) = join(listOf(*sources))

    fun inNewLine(source: Source) = NEW_LINE + source

    fun inParenthesesIfNotEmpty(source: Source) = source.ifNotEmpty { inParentheses(it) }

    fun inParentheses(source: Source) = join(LEFT_PARENTHESIS, source, RIGHT_PARENTHESIS)

    fun inAngleBrackets(source: Source) = join(LEFT_ANGLE_BRACKET, source, RIGHT_ANGLE_BRACKET)

    fun inSquareBrackets(source: Source) = join(LEFT_SQUARE_BRACKET, source, RIGHT_SQUARE_BRACKET)

    fun inDoubleQuotes(source: Source) = join(DOUBLE_QUOTE, source, DOUBLE_QUOTE)

    fun inCurlyBrackets(source: Source) = Source { sourceBuilder ->
      sourceBuilder.openBrace()
      sourceBuilder.append(source)
      sourceBuilder.closeBrace()
    }

    fun inInlineCurlyBrackets(source: Source) =
      spaceSeparated(LEFT_CURLY_BRACKET, source, RIGHT_CURLY_BRACKET)

    fun indented(source: Source) =
      emptyUnless(source.isNotEmpty()) {
        Source { sourceBuilder ->
          sourceBuilder.indent()
          sourceBuilder.append(source)
          sourceBuilder.unindent()
        }
      }

    fun indentedIf(condition: Boolean, source: Source) = if (condition) indented(source) else source

    fun spaceSeparated(sources: Iterable<Source>) = join(sources, separator = " ")

    fun commaSeparated(sources: Iterable<Source>) = join(sources, separator = ", ")

    fun commaAndNewLineSeparated(sources: Iterable<Source>) = join(sources, separator = ",\n")

    fun dotSeparated(sources: Iterable<Source>) = join(sources, separator = ".")

    fun ampersandSeparated(sources: Iterable<Source>) = join(sources, separator = " & ")

    fun semicolonSeparated(sources: Iterable<Source>) = join(sources, separator = "; ")

    fun colonSeparated(sources: Iterable<Source>) = join(sources, separator = ": ")

    fun newLineSeparated(sources: Iterable<Source>) = join(sources, separator = "\n")

    fun emptyLineSeparated(sources: Iterable<Source>) = join(sources, separator = "\n\n")

    fun spaceSeparated(source: Source, vararg sources: Source) =
      spaceSeparated(listOf(source, *sources))

    fun commaSeparated(source: Source, vararg sources: Source) =
      commaSeparated(listOf(source, *sources))

    fun dotSeparated(source: Source, vararg sources: Source) =
      dotSeparated(listOf(source, *sources))

    fun colonSeparated(source: Source, vararg sources: Source) =
      colonSeparated(listOf(source, *sources))

    fun newLineSeparated(source: Source, vararg sources: Source) =
      newLineSeparated(listOf(source, *sources))

    fun emptyLineSeparated(source: Source, vararg sources: Source) =
      emptyLineSeparated(listOf(source, *sources))

    fun infix(lhs: Source, operator: String, rhs: Source) = infix(lhs, source(operator), rhs)

    fun infix(lhs: Source, operator: Source, rhs: Source) = spaceSeparated(lhs, operator, rhs)

    fun block(body: Source) =
      if (body.isEmpty()) {
        inCurlyBrackets(EMPTY)
      } else {
        inCurlyBrackets(inNewLine(body))
      }

    fun block(firstLine: Source, body: Source) =
      if (firstLine.isEmpty()) {
        block(body)
      } else {
        inCurlyBrackets(newLineSeparated(join(SPACE, firstLine), body))
      }

    /** Appends the content of the given source to this source builder. */
    private fun SourceBuilder.append(source: Source) {
      source.nonEmptyAppendFn?.invoke(this)
    }
  }
}

/** Returns this source, or empty source if null. */
fun Source?.orEmpty() = this ?: Source.EMPTY
