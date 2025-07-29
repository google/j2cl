/*
 * Copyright 2017 Google Inc.
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
package jstypevarargs

import com.google.j2cl.integration.testing.Asserts.assertTrue

class QualifiedSuperMethodCall : Main(1) {

  inner class InnerClass {
    fun test1(): Int {
      // multiple arguments.
      return super@QualifiedSuperMethodCall.f3(1, 1, 2)
    }

    fun test2(): Int {
      // no argument for varargs.
      return super@QualifiedSuperMethodCall.f3(1)
    }

    fun test3(): Int {
      // array literal for varargs.
      return super@QualifiedSuperMethodCall.f3(1, *intArrayOf(1, 2))
    }

    fun test4(): Int {
      // empty array literal for varargs.
      return super@QualifiedSuperMethodCall.f3(1, *intArrayOf())
    }

    fun test5(): Int {
      // array object for varargs.
      val ints = intArrayOf(1, 2)
      return super@QualifiedSuperMethodCall.f3(1, *ints)
    }
  }

  companion object {
    fun test() {
      val i = QualifiedSuperMethodCall().InnerClass()
      assertTrue(i.test1() == 4)
      assertTrue(i.test2() == 1)
      assertTrue(i.test3() == 4)
      assertTrue(i.test4() == 1)
      assertTrue(i.test5() == 4)
    }
  }
}
