/*
 * Copyright 2025 Google Inc.
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
package forstatement

class ForStatement {
  fun test() {
    var count = 0

    for (i in 1..3) {
      count = i
    }

    for (i in 0 until 100) {
      count = i
    }

    for (c in 'a' until 'z') {}

    for (i in 6 downTo 0 step 2) {
      count = i
    }

    val list: List<Int> = ArrayList()
    for (l in list) {
      count = l
    }
    if (false) {
      for (i in 0..99) {}
    }

    for (i in 0..10) {
      for (j in 0 until 10) count = i + j
    }
  }
}
