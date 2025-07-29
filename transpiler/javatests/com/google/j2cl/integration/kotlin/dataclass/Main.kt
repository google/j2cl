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
package dataclass

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertNotEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testAccessors()
  testEquals()
  testHashCode()
  testCopy()
  testComponents()
  testToString()
  testInheritance()
  testArrayMembers()
}

data class Foo(val bar: Int, val buzz: String?, val optional: Int = 10) {
  var mutable = "test"
  val combined = "$bar + $buzz + $optional"

  fun hasBuzz() = buzz != null
}

fun testAccessors() {
  val foo = Foo(1, "someStr")

  assertEquals(1, foo.bar)
  assertEquals("someStr", foo.buzz)
  assertEquals(10, foo.optional)
  assertEquals("1 + someStr + 10", foo.combined)
  assertTrue(foo.hasBuzz())

  assertEquals("test", foo.mutable)
  foo.mutable = "other"
  assertEquals("other", foo.mutable)
}

fun testEquals() {
  val foo = Foo(1, "someStr")

  assertEquals(foo, Foo(1, "someStr"))
  assertEquals(foo, Foo(1, "someStr", 10))
  assertNotEquals(foo, Foo(2, "someStr"))
  assertNotEquals(foo, Foo(1, "otherStr"))
  assertNotEquals(foo, Foo(1, null))
  assertNotEquals(foo, Foo(2, "otherStr"))

  // Non-constructor properties are not considered for equality.
  val otherFoo = Foo(1, "someStr")
  otherFoo.mutable = "other"
  assertEquals(foo, otherFoo)
}

fun testHashCode() {
  val foo = Foo(1, "someStr")
  assertEquals(foo.hashCode(), Foo(1, "someStr").hashCode())

  // Non-constructor properties are not considered for hashcode.
  val otherFoo = Foo(1, "someStr")
  otherFoo.mutable = "other"
  assertEquals(foo.hashCode(), otherFoo.hashCode())
}

fun testCopy() {
  val foo = Foo(1, "someStr")

  assertEquals(foo, foo.copy())
  assertEquals(foo, foo.copy(bar = 1, buzz = "someStr"))
  assertNotEquals(foo, foo.copy(buzz = "otherStr"))
}

fun testComponents() {
  val (one, two, optional) = Foo(1, "someStr")

  assertEquals(1, one)
  assertEquals("someStr", two)
  assertEquals(10, optional)

  val foo = Foo(1, "someStr")
  assertEquals(1, foo.component1())
  assertEquals("someStr", foo.component2())
  assertEquals(10, foo.component3())
}

fun testToString() {
  val foo = Foo(1, "someStr")
  // Non-constructor properties are not considered for toString.
  assertEquals("Foo(bar=1, buzz=someStr, optional=10)", foo.toString())
}

data class ArrayMembers(private val a: IntArray, private val b: Array<String>)

fun testArrayMembers() {
  val arrayMembers = ArrayMembers(intArrayOf(1, 2, 3), arrayOf("a", "b"))

  assertEquals("ArrayMembers(a=[1, 2, 3], b=[a, b])", arrayMembers.toString())
  // Just make sure we get a non-zero hashCode.
  assertNotEquals(0, arrayMembers.hashCode())
}

interface InterfaceType {
  val a: Int
  var b: Int

  fun doSomething(): Int
}

abstract class AbstractType(open val a: Int, var c: Int) {
  abstract operator fun component2(): Int
}

data class Child(override val a: Int, override var b: Int) :
  AbstractType(a, a * -1), InterfaceType {
  override fun doSomething() = a + b

  operator fun component3() = c
}

fun testInheritance() {
  val child = Child(1, 2)

  assertEquals(1, child.a)
  assertEquals(2, child.b)
  assertEquals(-1, child.c)

  val (a, b, c) = child
  assertEquals(1, a)
  assertEquals(2, b)
  assertEquals(-1, c)

  assertEquals(child, Child(1, 2))

  val otherChild = Child(1, 2)
  otherChild.c = 3
  assertEquals(child, otherChild)
}
