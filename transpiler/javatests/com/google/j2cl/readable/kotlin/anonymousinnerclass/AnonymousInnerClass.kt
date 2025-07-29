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
package anonymousinnerclass

open class A {
  open inner class B
}

class AnonymousInnerClass {
  open inner class InnerClass

  fun test(arg: Int) {
    val ic: InnerClass = object : InnerClass() {} // new an anonymous class with implicit qualifier.
    val a = A()

    // new an anonymous class with explicit qualifier. Not supported in kotlin
    // val b: B = a.object : B() {}
    open class C {
      var fInC = arg
    }

    val c: C = object : C() {} // new an anonymous local class.
  }
}
