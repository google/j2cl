/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package inlineclass

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue

private interface I {
  val i: Int
}

@JvmInline
private value class Foo(override val i: Int) : I {
  fun plusOne() = Foo(i + 1)

  operator fun plus(other: Foo) = Foo(i + other.i)
}

fun main(vararg unused: String) {
  testParameterTypeResolution()
  testOperatorAndMethodCalls()
  testReturnTypeResolution()
  testEquality()
}

private fun testParameterTypeResolution() {
  val f = Foo(1)
  assertNonNullableEquals(1, f)
  assertNullableEquals(1, f)
  assertGenericEquals(1, f)
  assertObjectEquals(1, f)
  assertInterfaceEquals(1, f)
}

private fun assertNonNullableEquals(expected: Int, f: Foo) {
  assertEquals(expected, f.i)
}

private fun assertNullableEquals(expected: Int, f: Foo?) {
  assertEquals(expected, f!!.i)
}

private fun <T> assertGenericEquals(expected: Int, f: T) {
  assertEquals(expected, (f as Foo).i)
}

private fun assertObjectEquals(expected: Int, f: Any) {
  assertEquals(expected, (f as Foo).i)
}

private fun assertInterfaceEquals(expected: Int, f: I) {
  assertEquals(expected, f.i)
}

private fun testOperatorAndMethodCalls() {
  val f = Foo(1)
  assertEquals(2, f.i + f.i)
  assertEquals(2, f.plusOne().i)
  assertEquals(3, (f + Foo(2)).i)
}

private interface Factory<T> {
  fun create(i: Int): T
}

private class FooFactory : Factory<Foo> {
  override fun create(i: Int): Foo = Foo(i)
}

private fun testReturnTypeResolution() {
  val f = returnFoo(1)
  assertEquals(1, f.i)

  val ff = FooFactory()
  val f2 = ff.create(1)
  assertEquals(1, f2.i)
}

private fun returnFoo(i: Int): Foo {
  return Foo(i)
}

@JvmInline private value class FooEqTest(val i: Int)

private fun testEquality() {
  val f = Foo(1)
  val f2 = Foo(1)
  val f3 = Foo(2)
  val other = Any()
  val other2: Any = FooEqTest(1)

  assertTrue(f == f)
  assertTrue(f == f2)
  assertFalse(f == f3)
  assertFalse(f == other)
  // Ensure it property compares the underlying field for the correct type.
  assertFalse(f == other2)
}
