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
package java.util.regex

import kotlin.text.Regex

/** Kotlin Pattern implementation backed by kotlin.text.Regex. */
class Pattern(pattern: String?, private val flags: Int) {
  internal val regex: Regex
  init {
    try {
      regex = Regex(pattern!!, flagsToOptions(flags))
    } catch (e: RuntimeException) {
      throw PatternSyntaxException(e.message, pattern, -1)
    }
  }

  fun flags() = flags

  fun matcher(input: CharSequence?): Matcher = Matcher(this, input!!)

  fun split(input: CharSequence?): Array<String?>? = regex.split(input!!).toTypedArray()

  fun split(input: CharSequence?, limit: Int): Array<String?>? =
    regex.split(input!!, limit).toTypedArray()

  fun pattern() = regex.pattern

  override fun toString() = regex.toString()

  companion object {

    const val CANON_EQ = 128
    const val CASE_INSENSITIVE = 2
    const val COMMENTS = 4
    const val DOTALL = 32
    const val LITERAL = 16
    const val MULTILINE = 8
    const val UNICODE_CASE = 64
    const val UNICODE_CHARACTER_CLASS = 256
    const val UNIX_LINES = 1

    private fun flagsToOptions(flags: Int): Set<RegexOption> {
      val builder = mutableSetOf<RegexOption>()
      var flag = 1
      while (flag <= 256) {
        when (flags and flag) {
          0 -> {} // Flag not set
          CASE_INSENSITIVE -> builder.add(RegexOption.IGNORE_CASE)
          MULTILINE -> builder.add(RegexOption.MULTILINE)
          else -> throw IllegalStateException("Unsupported flag $flag")
        }
        flag *= 2
      }
      return builder.toSet()
    }

    fun compile(regex: String) = Pattern(regex, 0)

    fun compile(regex: String, flags: Int) = Pattern(regex, flags)

    fun matches(regex: String, input: CharSequence) = Regex(regex).matches(input)

    fun quote(s: String) = Regex.escape(s)
  }
}
