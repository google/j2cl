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
package com.google.j2cl.transpiler.backend.kotlin.ast

/** Kotlin keyword utilities. */
object Keywords {
  /**
   * Returns [true] if [string] is a hard keyword, as defined here:
   * https://kotlinlang.org/docs/keyword-reference.html#hard-keywords
   */
  fun isHard(string: String) = HARD_KEYWORD_SET.contains(string)

  /** Returns [true] if [string] keyword is not valid in enum value declaration. */
  fun isForbiddenInEnumValueDeclaration(string: String) = string == "init"

  // https://kotlinlang.org/docs/keyword-reference.html#hard-keywords
  private val HARD_KEYWORD_SET =
    setOf(
      "as",
      "as?",
      "break",
      "class",
      "continue",
      "do",
      "else",
      "false",
      "for",
      "fun",
      "if",
      "in",
      "interface",
      "!in",
      "is",
      "!is",
      "null",
      "object",
      "package",
      "return",
      "super",
      "this",
      "throw",
      "true",
      "try",
      "typealias",
      "typeof",
      "val",
      "var",
      "when",
      "while",
    )
}
