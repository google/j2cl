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
package rangeoperators

fun main() {
  if ('a' in 'a'..'z') {}

  if ('a' in 'a'..<'z') {}

  for (i in 'a'..'z') {}

  for (i in 'a'..<'z') {}

  for (i in 0..4) {}

  for (i in 0..<4) {}

  for (i in 0L..4L) {}

  for (i in 0L..<4L) {}
  // Different operand types.
  for (i in 0L..1) {}
  for (i in 0..1L) {}
  for (i in 0L..<1) {}
  for (i in 0..<1L) {}

  val s: Short = 1
  for (i in 1..s) {}
  for (i in s..1) {}
  for (i in 1L..s) {}
  for (i in s..1L) {}

  for (i in 1..<s) {}
  for (i in s..<1) {}
  for (i in 1L..<s) {}
  for (i in s..<1L) {}

  val b: Byte = 1
  for (i in 1..b) {}
  for (i in b..1) {}
  for (i in 1L..b) {}
  for (i in b..1L) {}
  for (i in 1..<b) {}
  for (i in b..<1) {}
  for (i in 1L..<b) {}
  for (i in b..<1L) {}

  for (i in 1..5 step 3) {}
  for (i in 1..<5 step 3) {}
  for (i in 1 until 5) {}
  for (i in 5 downTo 1 step 6) {}
  for (i in (1..5).reversed()) {}
  for (i in (1..<5).reversed()) {}

  // Unboxing
  val from: Short? = 1.toShort()
  val to: Long? = 1L
  if (from is Short && to is Long) {
    for (i in from..to) {}
    for (i in from..<to) {}
  }
}
