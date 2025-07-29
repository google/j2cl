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
package newinnerclasswithsameouter

import com.google.j2cl.integration.testing.Asserts.assertEquals

class EnclosingClass(var f: Int) {
  inner class A {
    fun test() {
      // New instance of other inner classes with the same outer class.
      val b = B()
      val c = C()
      assertEquals(10, b.get())
      assertEquals(20, c.get())
    }
  }

  inner class B {
    fun get(): Int {
      return f
    }
  }

  // private inner class
  private inner class C {
    fun get(): Int {
      return f * 2
    }
  }
}

fun main(vararg unused: String) {
  val m = EnclosingClass(10)
  val a: EnclosingClass.A = m.A()
  a.test()
}
