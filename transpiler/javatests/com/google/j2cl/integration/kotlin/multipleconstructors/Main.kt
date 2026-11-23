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
package multipleconstructors

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testMultipleConstructors()
  testNoPrimaryWithInitializers()
}

/** Test multiple constructors. */
class Foo {
  var id: Int
  var flag: Boolean

  constructor(id: Int) {
    this.id = id
    flag = id == 0
  }

  constructor(flag: Boolean) {
    id = -1
    this.flag = flag
  }

  constructor(id: Int, flag: Boolean) {
    this.id = id
    this.flag = flag
  }
}

class NoPrimaryWithInitializers {
  var order = "field"

  private val anonymous = run {
    order += "-anonymous"
    object {
      fun value() = 42
    }
  }

  init {
    order += "-init"
  }

  constructor(id: Int) {
    order += "-int"
  }

  constructor(flag: Boolean) {
    order += "-boolean"
  }

  constructor() : this(0) {
    order += "-noarg"
  }

  fun anonymousValue() = anonymous.value()

  fun anonymousClass() = anonymous::class.java
}

private fun testMultipleConstructors() {
  val o1 = Foo(1)
  assertTrue(o1.id == 1)
  assertFalse(o1.flag)

  val o2 = Foo(true)
  assertTrue(o2.id == -1)
  assertTrue(o2.flag)

  val o3 = Foo(10, false)
  assertTrue(o3.id == 10)
  assertFalse(o3.flag)
}

private fun testNoPrimaryWithInitializers() {
  val fromInt = NoPrimaryWithInitializers(1)
  val fromBoolean = NoPrimaryWithInitializers(true)
  val fromNoArg = NoPrimaryWithInitializers()
  assertEquals("field-anonymous-init-int", fromInt.order)
  assertEquals("field-anonymous-init-boolean", fromBoolean.order)
  assertEquals("field-anonymous-init-int-noarg", fromNoArg.order)

  assertEquals(42, fromInt.anonymousValue())
  assertEquals(42, fromBoolean.anonymousValue())
  assertTrue(fromInt.anonymousClass() === fromBoolean.anonymousClass())
}
