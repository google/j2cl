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
package equality

class Equality {
  fun test() {
    // Primitives
    val a = false === false
    val b = 0 !== 1

    // Objects
    val c = Any() !== Any()

    // Double
    val d = (0.0 as Double?) === (0.0 as Double?)

    // Float
    val e = (0.0f as Float?) === (0.0f as Float?)

    // Null literals
    val f = null !== Any()
    val g = Any() !== null
    val h = null !== IntArray(0)
  }
}
