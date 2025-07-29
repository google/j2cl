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
package interfacedevirtualize

class InterfaceDevirtualize {
  fun <T> compare0(c: Comparable<T>, t: T): Int {
    return c.compareTo(t)
  }

  fun compare1(c: Comparable<Any?>, o: Any?): Int {
    return c.compareTo(o)
  }

  fun compare2(c: Comparable<Double?>, d: Double?): Int {
    return c.compareTo(d)
  }

  fun compare3(d1: Double, d2: Double?) {
    d1.compareTo(d2!!)
    d1.compareTo(d2)
  }

  fun compare2(c: Comparable<Boolean?>, d: Boolean?): Int {
    return c.compareTo(d)
  }

  fun compare3(d1: Boolean, d2: Boolean?) {
    d1.compareTo(d2!!)
    d1.compareTo(d2)
  }

  fun compare2(c: Comparable<Int?>, d: Int?): Int {
    return c.compareTo(d)
  }

  fun compare3(d1: Int, d2: Int?) {
    d1.compareTo(d2!!)
    d1.compareTo(d2)
  }
}
