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
package simplelocalclass

class SimpleLocalClass {
  fun test(p: Int) {
    val localVar = 1

    class InnerClass {
      fun f(): Int {
        return localVar + p // captured local variable and parameter.
      }
    }
    InnerClass().f() // new local class.

    // local class that has no captured variables.
    class InnerClassWithoutCaptures
    InnerClassWithoutCaptures()
  }

  // Local class with same name.
  fun f() {
    val localVar = 1

    class InnerClass {
      var field = localVar
    }
  }

  // Local class with same name after $.
  fun foo() {
    class `Abc$InnerClass`
    class `Klm$InnerClass`
  }
}
