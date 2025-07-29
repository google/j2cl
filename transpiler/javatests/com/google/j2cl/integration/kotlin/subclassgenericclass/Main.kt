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
package subclassgenericclass

import com.google.j2cl.integration.testing.Asserts.assertTrue

open class Parent<T> {
  fun foo(t: T): T {
    return t
  }
}

class Child : Parent<Child>()

class GenericChild<T> : Parent<T>()

fun main(vararg unused: String) {
  val c = Child()
  val b: Child = c.foo(c)
  assertTrue(b === c)

  val gc = GenericChild<Child>()
  val cc: Child = gc.foo(c)
  assertTrue(cc === c)
}
