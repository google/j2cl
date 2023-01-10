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

// https://kotlinlang.org/docs/keyword-reference.html#hard-keywords
private val hardKeywordSet =
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
    "val",
    "var",
    "when",
    "while"
  )

// These keywords are forbidden for some reason or another and need to be mangled.
private val forbiddenKeywordSet =
  setOf(
    "delete", // Reserved word in ObjectiveC++
    "initialize", // Selector name in NSObject
    "scale", // Foundation method with conflicting return types
    "typeof", // Reserved word in swift.
    "BIG_ENDIAN", // Reserved as part of ObjectiveC on iOS see endian.h.
    "LITTLE_ENDIAN", // Reserved as part of ObjectiveC on iOS see endian.h.
    "NULL", // Reserved in stddef.h.
    "OVERFLOW", // Reserved in math.h
    "DOMAIN" // Reserved in math.h
  )

/**
 * Returns {@code true} if {@code string} is a hard keyword:
 * https://kotlinlang.org/docs/keyword-reference.html#hard-keywords
 */
fun isHardKeyword(string: String) = hardKeywordSet.contains(string)

fun isForbiddenKeyword(string: String) = forbiddenKeywordSet.contains(string)
