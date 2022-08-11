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
 * Pseudo-constructor for emulated java.lang.Double.
 *
 * See regular JRE API documentation for other methods in this file.
 */
operator fun Double.Companion.invoke(d: Double): Double = d

fun Double.Companion.valueOf(d: Double): Double = d

fun Double.Companion.valueOf(s: String): Double = s.toDouble()

fun Double.Companion.compare(d1: Double, d2: Double): Int = d1.compareTo(d2)

fun Double.Companion.toString(d: Double): String = d.toString()

fun Double.Companion.parseDouble(s: String): Double = s.toDouble()

fun Double.Companion.hashCode(d: Double): Int = d.hashCode()

fun Double.Companion.isNaN(d: Double): Boolean = d.isNaN()

fun Double.Companion.isInfinite(d: Double): Boolean = d.isInfinite()

fun Double.Companion.doubleToLongBits(value: Double): Long = value.toBits()

fun Double.Companion.doubleToRawLongBits(value: Double): Long = value.toRawBits()
