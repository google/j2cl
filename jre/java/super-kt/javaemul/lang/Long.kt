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
 * Pseudo-constructor for emulated java.lang.Long.
 *
 * See regular JRE API documentation for other methods in this file.
 */
operator fun Long.Companion.invoke(l: Long): Long = l

fun Long.Companion.valueOf(l: Long): Long = l

fun Long.Companion.valueOf(s: String): Long = s.toLong()

fun Long.Companion.valueOf(s: String, radix: Int): Long = s.toLong(radix)

fun Long.Companion.compare(l1: Long, l2: Long): Int = l1.compareTo(l2)

fun Long.Companion.toString(l: Long): String = l.toString()

fun Long.Companion.toString(l: Long, radix: Int): String = l.toString(radix)

fun Long.Companion.toHexString(l: Long): String = l.toULong().toString(16)

fun Long.Companion.parseLong(s: String): Long = s.toLong()

fun Long.Companion.parseLong(s: String, radix: Int): Long = s.toLong(radix)

fun Long.Companion.hashCode(l: Long): Int = l.hashCode()

fun Long.Companion.numberOfLeadingZeros(l: Long): Int = l.countLeadingZeroBits()

fun Long.Companion.numberOfTrailingZeros(l: Long): Int = l.countTrailingZeroBits()

fun Long.Companion.bitCount(l: Long): Int = l.countOneBits()

fun Long.Companion.signum(l: Long): Int = l.sign
