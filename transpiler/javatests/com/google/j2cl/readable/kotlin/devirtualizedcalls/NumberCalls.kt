/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package devirtualizedcalls

class NumberCalls {
  fun main() {
    val i: Number? = 1
    i?.toByte()
    i?.toDouble()
    i?.toFloat()
    i?.toInt()
    i?.toLong()
    i?.toShort()

    val ii: Int? = 1
    ii?.toByte()
    ii?.toDouble()
    ii?.toFloat()
    ii?.toInt()
    ii?.toLong()
    ii?.toShort()

    val d: Number? = 1.1
    d?.toByte()
    d?.toDouble()
    d?.toFloat()
    d?.toInt()
    d?.toLong()
    d?.toShort()

    val dd: Double? = 1.1
    dd?.toInt()
    dd?.toDouble()
    dd?.toFloat()
    dd?.toInt()
    dd?.toLong()
    dd?.toInt()

    val s: Number? = 1.toShort()
    s?.toByte()
    s?.toDouble()
    s?.toFloat()
    s?.toInt()
    s?.toLong()
    s?.toShort()

    val ss: Short? = 1.toShort()
    ss?.toByte()
    ss?.toDouble()
    ss?.toFloat()
    ss?.toInt()
    ss?.toLong()
    ss?.toShort()

    val b: Number? = 1.toByte()
    b?.toByte()
    b?.toDouble()
    b?.toFloat()
    b?.toInt()
    b?.toLong()
    b?.toShort()

    val bb: Byte? = 1.toByte()
    bb?.toByte()
    bb?.toDouble()
    bb?.toFloat()
    bb?.toInt()
    bb?.toLong()
    bb?.toShort()

    val f: Number? = 1.1f
    f?.toByte()
    f?.toDouble()
    f?.toFloat()
    f?.toInt()
    f?.toLong()
    f?.toShort()

    val ff: Float? = 1.1f
    ff?.toInt()
    ff?.toDouble()
    ff?.toFloat()
    ff?.toInt()
    ff?.toLong()
    ff?.toInt()

    val l: Number? = 1L
    l?.toByte()
    l?.toDouble()
    l?.toFloat()
    l?.toInt()
    l?.toLong()
    l?.toShort()

    val ll: Long? = 1L
    ll?.toByte()
    ll?.toDouble()
    ll?.toFloat()
    ll?.toInt()
    ll?.toLong()
    ll?.toShort()

    val c: Char? = 'a'
    c?.toChar()
    c?.toByte()
    c?.toDouble()
    c?.toFloat()
    c?.toInt()
    c?.toLong()
    c?.toShort()

    val bool: Boolean = true
    // Kotlin Boolean has no equivalent booleanValue
  }
}
