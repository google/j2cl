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
package stringtemplate

fun foo(a: String, b: Int) = a + b

fun bar(a: String, b: Int) = "$a $b"

fun buzz() = "some wrapping ${(foo("fooA", 2) + bar("barA", 3))} text"

@JvmInline value class SomeValueType(private val i: Int)

fun templateWithValueType() = "some wrapping ${SomeValueType(1)} text"
