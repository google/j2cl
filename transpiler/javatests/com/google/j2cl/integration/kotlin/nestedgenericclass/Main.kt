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
package nestedgenericclass

import com.google.j2cl.integration.testing.Asserts.assertSame
import com.google.j2cl.integration.testing.Asserts.assertTrue

class EnclosingClass<T> {
  fun <T> function(param: T) {
    /** Generic local class declaring a shadow type variable. */
    class A<T>(var t: T)

    /** Non-generic local class that infers type variable declared in enclosing method. */
    class B(var t: T)

    // new A<> with different type arguments.
    assertTrue(A(Error()).t is Error)
    assertTrue(A(Exception()).t is Exception)
    assertSame(param, B(param).t)
  }
}

fun main(vararg unused: String) {
  val m = EnclosingClass<Any>()
  // invokes local classes' creation with different types of arguments.
  m.function(Any())
  m.function(Error())
  m.function(Exception())
  m.function(m)
}
