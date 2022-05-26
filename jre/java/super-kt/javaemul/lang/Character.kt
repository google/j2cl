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
package javaemul.lang

/**
 * Pseudo-constructor for emulated java.lang.Character.
 *
 * See regular JRE API documentation for other methods in this file.
 */
operator fun Char.Companion.invoke(c: Char): Char = c

// TODO(b/233944334): Duplicate method for JVM.
val Char.Companion.MIN_SUPPLEMENTARY_CODE_POINT: Int
  inline get() = 0x10000

fun Char.Companion.valueOf(c: Char): Char = c

fun Char.Companion.compare(c1: Char, c2: Char): Int = c1.compareTo(c2)

fun Char.Companion.forDigit(digit: Int, radix: Int): Char = digit.digitToChar(radix).lowercaseChar()

fun Char.Companion.hashCode(c: Char): Int = c.hashCode()

fun Char.Companion.charCount(codePoint: Int): Int =
  if (codePoint >= MIN_SUPPLEMENTARY_CODE_POINT) 2 else 1

fun Char.Companion.codePointAt(charSequence: CharArray?, index: Int, limit: Int): Int {
  requireNotNull(charSequence)

  var hiSurrogate = charSequence[index]
  var loSurrogate = charSequence[index + 1]
  if (isSurrogatePair(hiSurrogate, loSurrogate) && index < limit) {
    return toCodePoint(hiSurrogate, loSurrogate)
  }
  return hiSurrogate.code
}

fun Char.Companion.isSurrogatePair(high: Char, low: Char): Boolean =
  high.isHighSurrogate() && low.isLowSurrogate()

// TODO(b/233944334): Duplicate method for JVM.
fun Char.Companion.toCodePoint(high: Char, low: Char): Int =
  (((high - MIN_HIGH_SURROGATE) shl 10) or (low - MIN_LOW_SURROGATE)) + MIN_SUPPLEMENTARY_CODE_POINT

fun Char.Companion.toChars(codePoint: Int, dst: CharArray?, dstIndex: Int): Int {
  // TODO(b/228304843): Add InternalPreconditions for Kotlin.
  // checkCriticalArgument(codePoint >= 0 && codePoint <= MAX_CODE_POINT);
  requireNotNull(dst)

  if (codePoint >= MIN_SUPPLEMENTARY_CODE_POINT) {
    dst[dstIndex] =
      (MIN_HIGH_SURROGATE + (((codePoint - MIN_SUPPLEMENTARY_CODE_POINT) shr 10) and 1023)).toChar()
    dst[dstIndex + 1] =
      (MIN_LOW_SURROGATE + ((codePoint - MIN_SUPPLEMENTARY_CODE_POINT) and 1023)).toChar()
    return 2
  }
  dst[dstIndex] = codePoint.toChar()
  return 1
}
