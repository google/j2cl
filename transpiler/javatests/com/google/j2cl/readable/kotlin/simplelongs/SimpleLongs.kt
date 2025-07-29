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
package simplelongs

class SimpleLongs {
  var foo: Long = 0

  fun getBar(): Long {
  return 0
}

  private var sideEffect = 0

  fun getWithSideEffect(): SimpleLongs {
    sideEffect++
    return this
  }

  fun main() {
    // Small literals.
    var a = 0L
    a = -100000L
    a = 100000L

    // Larger than int literals.
    var b = -2147483648L
    b = 2147483648L
    // kotlin does not allow to express Long.MIN_VALUE (-9223372036854775808L)  as a literal
    b = -9223372036854775807L - 1L
    b = 9223372036854775807L

    // Binary expressions.
    var c: Long = a + b
    c = a / b

    // Prefix expressions;
    var e: Long = ++a
    e = ++foo
    e = ++getWithSideEffect().foo

    // Postfix expressions.
    var f: Long = a++
    f = foo++
    f = getWithSideEffect().foo++

    // Field initializers and function return statements.
    var g: Long = foo
    g = getBar()
  }
}
