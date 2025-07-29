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
package localclasswithconstructor

class LocalClassWithConstructor {
  fun test(p: Int) {

    var localVar = 1

    // local class with non-default constructor and this() call
    class LocalClass {
      var field: Int

      constructor(a: Int, b: Int) {
        field = localVar + a + b
        localVar = field
      }

      constructor(a: Int) : this(a, p) {
        field = localVar
      }
    }

    val a = LocalClass(1)

    localVar = a.field
  }
}

fun test() {
  var x = 3

  class LocalClass(val i: Int) {
    val y = x
  }
}
