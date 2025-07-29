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
package simpleliterals

class SimpleLiterals {
  fun main() {
    val bool = false
    val ch = 'a'
    val b: Byte = 127
    val nb: Byte = -128
    val s: Short = 32767
    val ns: Short = -32768
    val i = 101
    val ni = -101
    val l = 101L
    val nl = -101L
    val f = 101.0f
    val nf = -101.0f
    val zf = 0f
    val nzf = -0f
    val d = 101.0
    val nd = -101.0
    val zd = 0.0
    val nzd = -0.0
    val o: Any? = null
    val str = "foo"
    val c = SimpleLiterals::class
    val zeroF = 0.0f
    val minusZeroF = -0.0f
    val zeroD = 0.0
    val minusZeroD = -0.0
    val minusMinusZeroD = - /* see b/235149840 */ -0.0
  }
}
