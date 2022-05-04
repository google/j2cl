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

import kotlin.math.sign

/**
 * Pseudo-constructor for emulated java.lang.Int.
 *
 * See regular JRE API documentation for other methods in this file.
 */
operator fun Int.Companion.invoke(i: Int): Int = i

fun Int.Companion.valueOf(i: Int): Int = i

fun Int.Companion.valueOf(s: String): Int = s.toInt()

fun Int.Companion.valueOf(s: String, radix: Int): Int = s.toInt(radix)

fun Int.Companion.compare(i1: Int, i2: Int): Int = i1.compareTo(i2)

fun Int.Companion.toString(i: Int): String = i.toString()

fun Int.Companion.toString(i: Int, radix: Int): String = i.toString(radix)

fun Int.Companion.toHexString(i: Int): String = i.toUInt().toString(16)

fun Int.Companion.parseInt(s: String): Int = s.toInt()

fun Int.Companion.parseInt(s: String, radix: Int): Int = s.toInt(radix)

fun Int.Companion.hashCode(i: Int): Int = i.hashCode()

fun Int.Companion.numberOfLeadingZeros(i: Int): Int = i.countLeadingZeroBits()

fun Int.Companion.numberOfTrailingZeros(i: Int): Int = i.countTrailingZeroBits()

fun Int.Companion.highestOneBit(i: Int): Int = i.takeHighestOneBit()

fun Int.Companion.bitCount(i: Int): Int = i.countOneBits()

fun Int.Companion.signum(i: Int): Int = i.sign
