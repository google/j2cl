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

/**
 * Returns escape identifier for this char if present
 * (https://kotlinlang.org/spec/syntax-and-grammar.html#grammar-rule-EscapedIdentifier).
 */
private val Char.escapeIdentifierCharOrNull: Char?
  get() =
    when (this) {
      '\\' -> '\\'
      '\t' -> 't'
      '\b' -> 'b'
      '\n' -> 'n'
      '\r' -> 'r'
      '\'' -> '\''
      '"' -> '"'
      '$' -> '$'
      else -> null
    }

/** Returns escaped string using escape identifier char if possible. */
private val Char.identifierEscapedStringOrNull: String?
  get() = escapeIdentifierCharOrNull?.let { "\\$it" }

/** Returns true if this char should be escaped using Unicode escaping. */
private val Char.needsUnicodeEscaping: Boolean
  get() = toInt().let { it < 0x20 || it >= 0x7f }

/**
 * Returns escaped string using Unicode escaping, or null if it does not need to be escaped
 * (https://kotlinlang.org/spec/syntax-and-grammar.html#grammar-rule-UniCharacterLiteral).
 */
private val Char.unicodeEscapedStringOrNull: String?
  get() = takeIf { it.needsUnicodeEscaping }?.let { String.format("\\u%04X", it.toInt()) }

/** Returns escaped string. */
val Char.escapedString: String
  get() = identifierEscapedStringOrNull ?: unicodeEscapedStringOrNull ?: toString()

/** Returns escaped string. */
val String.escapedString: String
  get() =
    // Surrogate pairs must be escaped separately in Kotlin, so escaping each Char separately is OK.
    StringBuilder().also { builder -> forEach { builder.append(it.escapedString) } }.toString()

val Char.literalString: String
  get() = "'$escapedString'"

val String.literalString: String
  get() = "\"$escapedString\""
