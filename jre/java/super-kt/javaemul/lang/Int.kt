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
 * Pseudo-constructor for emulated java.lang.Int.
 *
 * See regular JRE API documentation for other methods in this file.
 */
operator fun Int.Companion.invoke(i: Int) = i

fun Int.Companion.valueOf(i: Int) = i

fun Int.Companion.compare(i1: Int, i2: Int) = i1.compareTo(i2)

fun Int.Companion.toHexString(i: Int) = i.toString(16)
