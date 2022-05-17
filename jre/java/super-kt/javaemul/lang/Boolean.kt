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
 * Pseudo-constructor for emulated java.lang.Boolean.
 *
 * See regular JRE API documentation for other methods in this file.
 */
operator fun Boolean.Companion.invoke(b: Boolean): Boolean = b

val Boolean.Companion.TRUE: Boolean
  inline get() = true

val Boolean.Companion.FALSE: Boolean
  inline get() = false

fun Boolean.Companion.valueOf(b: Boolean): Boolean = b

fun Boolean.booleanValue(): Boolean = this

fun Boolean.Companion.hashCode(b: Boolean): Int = b.hashCode()

fun Boolean.Companion.toString(b: Boolean): String = b.toString()
