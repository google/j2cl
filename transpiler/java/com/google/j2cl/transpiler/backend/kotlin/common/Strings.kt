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
package com.google.j2cl.transpiler.backend.kotlin.common

/**
 * Returns whether this camel-case string starts with the given prefix, followed by a non-empty
 * substring.
 */
fun String.camelCaseStartsWith(prefix: String): Boolean =
  startsWith(prefix) && getOrNull(prefix.length).run { this != null && !isLowerCase() }

/** Returns title-cased version of this string. */
val String.titleCased: String
  get() = replaceFirstChar { it.toUpperCase() }

/** Returns this string in single quotes. */
val String.inSingleQuotes: String
  get() = "'$this'"

/** Returns this string in back-ticks. */
val String.inBackTicks: String
  get() = "`$this`"

/** Returns this string in double quotes. */
val String.inDoubleQuotes: String
  get() = "\"$this\""
