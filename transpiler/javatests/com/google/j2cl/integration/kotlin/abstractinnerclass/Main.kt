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
package abstractinnerclass

import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  assertTrue(10 == Main.BB().bar())
  assertTrue(20 == Main().CC().bar())
}

class Main {
  internal interface A {
    fun foo(): Int
  }

  internal abstract class B : A {
    fun bar(): Int {
      return foo()
    }
  }

  internal abstract inner class C : A {
    fun bar(): Int {
      return foo()
    }
  }

  internal class BB : B() {
    override fun foo(): Int {
      return 10
    }
  }

  internal inner class CC : C() {
    override fun foo(): Int {
      return 20
    }
  }
}
