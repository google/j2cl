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
package simpleliterals

import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  val a = false
  assertTrue(!a)

  val b = 'a'
  assertTrue(b.code == 97)

  // Verify compile time float literal expansion to double.
  assertTrue(0.7308782f == 0.7308781743049622.toFloat())

  // Avoiding a "condition always evaluates to true" error in JSComp type checking.
  val maybeNull = if (b.code == 97) null else Any()
  val c: Any? = null
  assertTrue(c === maybeNull)

  val d = 100
  assertTrue(d == 50 + 50)

  val e = "foo"
  assertTrue(e is String)

  val f: Class<*> = Foo::class.java
  assertTrue(f is Class<*>)
}

private class Foo {}
