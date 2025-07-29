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
package inoperator

class Holder(val content: Int) {
  operator fun contains(other: Int): Boolean {
    return content == other
  }
}

fun main() {
  val a = 1 in Holder(1)
  val b = 1 !in Holder(2)

  var c =
    when (1) {
      in Holder(1) -> "In Holder"
      !in Holder(1) -> "Out of Holder"
      else -> "In Holder"
    }

  for (item in intArrayOf(1, 2, 3)) {}
}
