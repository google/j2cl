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

import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.charset.UnsupportedCharsetException
import java.util.Locale

/**
 * Pseudo-constructor for emulated java.lang.String.
 *
 * We assume that the platfrom encoding is UTF-8 here (like J2Objc and Android do).
 *
 * See regular JRE API documentation for other methods in this file.
 */
operator fun String.Companion.invoke(a: CharArray?): String {
  requireNotNull(a)
  return a.concatToString()
}

operator fun String.Companion.invoke(a: CharArray?, offset: Int, len: Int): String {
  requireNotNull(a)
  return a.concatToString(offset, offset + len)
}

operator fun String.Companion.invoke(a: ByteArray?) =
  a!!.decodeToString(throwOnInvalidSequence = false)

operator fun String.Companion.invoke(a: ByteArray?, offset: Int, len: Int) =
  a!!.decodeToString(offset, offset + len, throwOnInvalidSequence = false)

operator fun String.Companion.invoke(
  a: ByteArray?,
  offset: Int,
  len: Int,
  charSet: Charset?
): String {
  if (charSet!! != StandardCharsets.UTF_8) {
    throw UnsupportedEncodingException(charSet.name())
  }
  return a!!.decodeToString(offset, offset + len, throwOnInvalidSequence = true)
}

operator fun String.Companion.invoke(a: ByteArray?, charSet: Charset?) =
  String(a, 0, a!!.size, charSet)

operator fun String.Companion.invoke(a: ByteArray?, charSetName: String?) =
  String(a!!, 0, a!!.size, charSetName)

operator fun String.Companion.invoke(
  a: ByteArray?,
  offset: Int,
  len: Int,
  charsetName: String?
): String {
  try {
    return String(a, offset, len, Charset.forName(charsetName))
  } catch (e: UnsupportedCharsetException) {
    throw UnsupportedEncodingException(charsetName)
  }
}

fun String.Companion.valueOf(c: Char): String = c.toString()

fun String.Companion.valueOf(a: Any?): String = a.toString()

internal fun String.Companion.fromCodePoint(codePoint: Int): String {
  if (codePoint >= Char.MIN_SUPPLEMENTARY_CODE_POINT) {
    val chars = CharArray(2)
    Char.toChars(codePoint, chars, 0)
    return chars.concatToString()
  } else {
    return codePoint.toChar().toString()
  }
}

fun String.compareToIgnoreCase(str: String): Int = this.compareTo(str, ignoreCase = true)

fun String.getBytes() = encodeToByteArray()

fun String.getBytes(charsetName: String): ByteArray {
  try {
    return getBytes(Charset.forName(charsetName))
  } catch (e: UnsupportedCharsetException) {
    throw UnsupportedEncodingException(charsetName)
  }
}

fun String.getBytes(charset: Charset?): ByteArray {
  if (charset!! != StandardCharsets.UTF_8) {
    throw UnsupportedEncodingException(charset.name())
  }
  return encodeToByteArray()
}

// TODO(b/230671584): Add support for Locale on Kotlin Native
fun String.toUpperCase(locale: Locale?): String = this.uppercase()

fun String.toLowerCase(locale: Locale?): String = this.lowercase()

fun String.getChars(start: Int, end: Int, buffer: CharArray, index: Int) {
  requireNotNull(buffer)

  var bufferIndex = index
  for (srcIndex in start until end) {
    buffer[bufferIndex++] = this[srcIndex]
  }
}

// TODO(b/233944334): This duplicates JRE code on the JVM
fun String.indexOf(codePoint: Int, fromIndex: Int = 0): Int {
  if (codePoint < Char.MIN_SUPPLEMENTARY_CODE_POINT) {
    return indexOf(codePoint.toChar(), fromIndex)
  } else {
    return indexOf(String.fromCodePoint(codePoint), fromIndex)
  }
}

// TODO(b/233944334): This duplicates JRE code on the JVM
fun String.lastIndexOf(codePoint: Int, fromIndex: Int = Int.MAX_VALUE): Int {
  if (codePoint < Char.MIN_SUPPLEMENTARY_CODE_POINT) {
    return lastIndexOf(codePoint.toChar(), fromIndex)
  } else {
    return lastIndexOf(String.fromCodePoint(codePoint), fromIndex)
  }
}

fun String.java_split(regularExpression: String): Array<String?>? {
  val strList: List<String> = this.split(regularExpression.toRegex())
  return strList.toTypedArray()
}

fun String.java_split(regularExpression: String, limit: Int): Array<String?>? {
  val strList: List<String> = this.split(regularExpression.toRegex(), limit)
  return strList.toTypedArray()
}

fun String.java_replace(target: CharSequence?, replacement: CharSequence?): String {
  return this.replace(target.toString(), replacement.toString())
}
