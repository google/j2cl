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
package localclassinstaticcontext

var f: Any = object : Any() {} // Initializer in a static field declaration.

fun test() {
  // In a static function.
  class A
  A()
  val a: Any = object : Any() {
    fun m() {
      // Here it is created in an instance context but the class is defined in a static one,
      // so no extra outer_this.
      A()
    }
  }
}

class LocalClassInStaticContext {
  // There is no static static initialization block in kotlin
  // companion object {
  //   In a static block.
  //   init {
  //     class B
  //     B()
  //     val b: Any = object : Any() {}
  //   }
  // }

  class C {
    var f = 1
    fun test() {
      // It should have an enclosing instance, although its outer class is static.
      class D {
        fun func(): Int {
          return f
        }
      }
      D().func()
    }
  }
}
