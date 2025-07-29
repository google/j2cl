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
package voidtypes

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertSame
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException

fun main(vararg unused: String) {
  testUnitVariable()
  testUnitVariable()
  testUnitGetterWithSideEffects()
  testNothingCast()
  testNothingIs()
}

fun unitFun(): Unit {}

fun testUnitFun() {
  assertSame(Unit, unitFun())
}

fun testUnitVariable() {
  val foo = unitFun()
  assertSame(Unit, foo)
}

val unitField: Unit = Unit
val unitFieldFromFun: Unit = unitFun()
val unitFieldGetter: Unit
  get() = Unit

fun testUnitField() {
  assertSame(Unit, unitField)
  assertSame(Unit, unitFieldFromFun)
  assertSame(Unit, unitFieldGetter)
}

var counter = 0
val foo: Unit
  get() {
    counter++
    return Unit
  }

fun testUnitGetterWithSideEffects() {
  val unused = foo
  assertEquals(1, counter)
}

fun returnUnitBlock(a: Int): Unit =
  when (a) {
    1 -> unitFun()
    else -> unitFun()
  }

fun testUnitBlock() {
  assertSame(Unit, returnUnitBlock(1))
  assertSame(Unit, returnUnitBlock(2))
  val foo =
    when (counter) {
      else -> Unit
    }
  assertSame(Unit, foo)
}

fun testNothingCast() {
  assertThrowsClassCastException {
    val x = 1 as Nothing
  }
}

fun testNothingIs() {
  assertFalse(1 is Nothing)
}
