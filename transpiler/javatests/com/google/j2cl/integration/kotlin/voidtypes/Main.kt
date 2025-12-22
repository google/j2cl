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
import jsinterop.annotations.JsFunction

fun main(vararg unused: String) {
  testUnitFun()
  testUnitVariable()
  testUnitGetterWithSideEffects()
  testGenericUnitLambdas()
  testUnitBlock()
  testUnitSpecialization()
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

fun testGenericUnitLambdas() {
  // TODO(b/466125631): enable these tests once the bug is fixed.
  // genericFunExpectingUnitBlock {}
  // genericFunExpectingUnitBlock {
  //   return@genericFunExpectingUnitBlock
  // }
  // genericFunExpectingUnitBlock(::unitFun)
  genericFunExpectingUnitBlock(::unitField)
  // TODO(b/466125631): enable these tests once the bug is fixed.
  // genericFunExpectingJsFunctionReturningUnit {}
  // genericFunExpectingJsFunctionReturningUnit {
  //   return@genericFunExpectingJsFunctionReturningUnit
  // }
  // genericFunExpectingJsFunctionReturningUnit(::unitFun)*/
  genericFunExpectingJsFunctionReturningUnit(::unitField)
}

private fun <T> genericFunExpectingUnitBlock(block: () -> T) {
  assertSame(Unit, block())
}

@JsFunction
fun interface GenericJsFunction<T> {
  fun execute(): T
}

private fun <T> genericFunExpectingJsFunctionReturningUnit(returnUnit: GenericJsFunction<T>) {
  assertSame(Unit, returnUnit.execute())
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

fun testUnitSpecialization() {
  // TODO(b/466125631): enable these tests once the bug is fixed.
  // val unitHolder = UnitHolder(Unit)
  // assertSame(Unit, unitHolder.getValue())
  // val holder: Holder<out Unit> = UnitHolder(Unit)
  // assertSame(Unit, holder.getValue())
  //
  // val unitList = UnitList()
  // unitList.add(Unit)
  // unitList.add(Unit)
  // unitList.add(Unit)
  //
  // assertEquals(3, unitList.size)
  // assertTrue(unitList.contains(Unit))
  // assertEquals(0, unitList.indexOf(Unit))
  // assertEquals(2, unitList.lastIndexOf(Unit))
  // assertSame(Unit, unitList[1])
  //
  // val anyList = unitList as List<Any>
  // assertFalse(anyList.contains(Any()))
  // assertTrue(anyList.contains(Unit))
  // assertEquals(-1, unitList.indexOf(Any()))
  // assertEquals(0, unitList.indexOf(Unit))
}

// TODO(b/466125631): enable these tests once the bug is fixed.
// private interface Holder<T> {
//   fun getValue(): T
// }
//
// private class UnitHolder(private val v: Unit) : Holder<Unit> {
//   override fun getValue() = v
// }
//
// class UnitList : ArrayList<Unit>() {
//   override fun lastIndexOf(element: Unit): Int = super.lastIndexOf(element)
//
//   override fun indexOf(element: Unit): Int = super.indexOf(element)
//
//   override fun containsAll(elements: Collection<Unit>): Boolean = super.containsAll(elements)
//
//   override fun contains(element: Unit): Boolean = super.contains(element)
//
//   override fun removeAt(element: Int): Unit = super.removeAt(element)
//
//   override fun get(index: Int): Unit = super.get(index)
// }

fun testNothingCast() {
  assertThrowsClassCastException {
    val x = 1 as Nothing
  }
}

fun testNothingIs() {
  assertFalse(1 is Nothing)
}
