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
 * Pseudo-constructor for emulated java.lang.Short.
 *
 * See regular JRE API documentation for other methods in this file.
 */
operator fun Short.Companion.invoke(s: Short): Short = s

fun Short.Companion.valueOf(s: Short): Short = s

fun Short.Companion.valueOf(str: String): Short = str.toShort()

fun Short.Companion.valueOf(s: String, radix: Int): Short = s.toShort(radix)

fun Short.Companion.compare(s1: Short, s2: Short): Int = s1.compareTo(s2)

fun Short.Companion.toString(s: Short): String = s.toString()

fun Short.Companion.parseShort(str: String): Short = str.toShort()

fun Short.Companion.parseShort(s: String, radix: Int): Short = s.toShort(radix)

fun Short.Companion.hashCode(s: Short): Int = s.hashCode()
