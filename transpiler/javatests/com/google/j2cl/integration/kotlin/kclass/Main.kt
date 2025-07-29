/*
 * Copyright 2023 Google Inc.
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
package kclass

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertNotEquals
import com.google.j2cl.integration.testing.Asserts.assertNotSame
import com.google.j2cl.integration.testing.TestUtils
import kotlin.reflect.KClass

fun main(vararg unused: String) {
  testInstanceEquality()
  testPrimitive()
  testInterface()
  testInstanceType()
  testArray()
  testObjects()
  testInlineClass()
}

private fun <T : Any> getKClass(ref: T): KClass<out T> =
  // Reference the KClass generically so that the compiler cannot eagerly optimize the code.
  ref::class

private fun testInstanceEquality() {
  assertEquals(Int::class, Int::class)
  assertNotSame(Int::class, Int::class)

  assertEquals(Any::class, Any::class)
  assertNotSame(Any::class, Any::class)
}

private fun testPrimitive() {
  assertEquals(Int::class, Int::class)
  assertEquals(Int::class, 1::class)
  assertEquals(Int::class, getKClass(1))

  var i = 1
  assertEquals(Int::class, i::class)
  assertEquals(Int::class, getKClass(i))

  // Side-effects should be preserved
  assertEquals(Int::class, (++i)::class)
  assertEquals(2, i)

  val nullableI: Int? = 1
  assertEquals(Int::class, nullableI!!::class)
  assertEquals(Int::class, getKClass(nullableI!!))

  assertClassToStringEquals("int", 1::class.toString())
  assertClassToStringEquals("class java.lang.Integer", nullableI!!::class.toString())
}

interface IFoo

private fun testInterface() {
  assertEquals(IFoo::class, IFoo::class)
  assertNotEquals(IFoo::class, object : IFoo {}::class)
  assertClassToStringEquals("interface kclass.IFoo", IFoo::class.toString())
}

open class ConcreteType

private fun testInstanceType() {
  assertEquals(ConcreteType::class, ConcreteType()::class)
  assertEquals(ConcreteType::class, getKClass(ConcreteType()))
  val o = ConcreteType()
  assertEquals(ConcreteType::class, o::class)
  assertEquals(ConcreteType::class, getKClass(o))

  assertNotEquals(ConcreteType::class, object : ConcreteType() {}::class)

  assertClassToStringEquals("class kclass.ConcreteType", ConcreteType::class.toString())
}

private fun testArray() {
  assertEquals(IntArray::class, intArrayOf()::class)
  assertEquals(IntArray::class, getKClass(intArrayOf()))
  assertEquals(Array<Int>::class, arrayOf<Int>()::class)
  assertEquals(Array<Int>::class, getKClass(arrayOf<Int>()))
  assertNotEquals(IntArray::class, Array<Int>::class)

  assertEquals(Array<ConcreteType>::class, arrayOf<ConcreteType>()::class)
  assertEquals(Array<ConcreteType>::class, getKClass(arrayOf<ConcreteType>()))

  assertEquals(Array<Array<ConcreteType>>::class, arrayOf<Array<ConcreteType>>()::class)
  assertEquals(Array<Array<ConcreteType>>::class, getKClass(arrayOf<Array<ConcreteType>>()))

  assertClassToStringEquals("class [Lkclass.ConcreteType;", Array<ConcreteType>::class.toString())
  assertClassToStringEquals("class [I", IntArray::class.toString())

  assertClassToStringEquals(
    "class [[Lkclass.ConcreteType;",
    Array<Array<ConcreteType>>::class.toString(),
  )
  assertClassToStringEquals("class [[I", Array<IntArray>::class.toString())
}

@Suppress("ClassShouldBeObject")
class Foo {
  companion object {}
}

private fun testObjects() {
  var o: Any = object {}
  assertEquals(o::class, o::class)
  assertNotEquals(o::class, object {}::class)
  if (TestUtils.isJvm()) {
    assertClassToStringEquals("class kclass.MainKt\$testObjects\$o\$1", o::class.toString())
  } else {
    assertClassToStringEquals("class kclass.MainKt\$3", o::class.toString())
  }

  o = Foo.Companion
  assertEquals(Foo.Companion::class, o::class)
  assertNotEquals(Foo.Companion::class, object {}::class)
  assertClassToStringEquals("class kclass.Foo\$Companion", Foo.Companion::class.toString())
}

@JvmInline value class InlineClass(val value: Int) {}

private fun testInlineClass() {
  assertEquals(InlineClass::class, InlineClass(1)::class)
  assertEquals(InlineClass::class, getKClass(InlineClass(1)))
  assertClassToStringEquals("class kclass.InlineClass", InlineClass::class.toString())
}

private fun assertClassToStringEquals(expected: String, actual: String) {
  // Kotlin JVM suffixes all KClass#toString calls with this when the kotlin-reflect library is not
  // linked. J2CL doesn't emulate this for code size concerns so we should only expect it when
  // running on the JVM.
  val toStringSuffix = if (TestUtils.isJvm()) " (Kotlin reflection is not available)" else ""
  assertEquals("$expected$toStringSuffix", actual)
}
