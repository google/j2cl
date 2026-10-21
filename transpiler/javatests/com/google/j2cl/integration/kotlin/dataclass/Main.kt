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
import com.google.j2cl.integration.testing.TestUtils.isJavaScript

fun main(vararg unused: String) {
  testAccessors()
  testEquals()
  testHashCode()
  testCopy()
  testComponents()
  testToString()
  testInterfaceDelegation()
  testSecondaryConstructor()
  testInheritance()
  testLocalDataClassWithCaptures()
  testOverriddenObjectMethods()
  testArrayMembers()
  testFloatingPoint()
  testJvmRecordDataClass()
}

data class Foo(val bar: Int, val buzz: String?, var optional: Int = 10) {
  var mutable = "test"
  var withCustomSetter = 0
    set(value) {
      field = value + 1
    }

  @JvmField var jvmField = 100
  private var privateField: Int? = null
  lateinit var lateinitField: String
  val combined = "$bar + $buzz + $optional"
  val lazyDelegated: String by lazy { mutable }

  init {
    withCustomSetter = bar
  }

  fun hasBuzz() = buzz != null

  fun setPrivateField(value: Int) {
    privateField = value
  }
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

  assertEquals(2, foo.withCustomSetter)
  foo.withCustomSetter = 2
  assertEquals(3, foo.withCustomSetter)

  assertEquals(100, foo.jvmField)
  foo.jvmField = 101
  assertEquals(101, foo.jvmField)

  foo.lateinitField = "lateinit"
  assertEquals("lateinit", foo.lateinitField)

  // Lazy properties are initialized on first access.
  assertEquals("other", foo.lazyDelegated)
}

fun testEquals() {
  val foo = Foo(1, "someStr")

  assertEquals(foo, Foo(1, "someStr"))
  assertEquals(foo, Foo(1, "someStr", 10))
  assertNotEquals(foo, Foo(2, "someStr"))
  assertNotEquals(foo, Foo(1, "otherStr"))
  assertNotEquals(foo, Foo(1, null))
  assertNotEquals(foo, Foo(2, "otherStr"))

  // Mutable constructor properties are considered for equality.
  assertNotEquals(foo, Foo(1, "someStr").apply { optional = 20 })

  // Non-constructor properties are not considered for equality.
  assertEquals(foo, Foo(1, "someStr").apply { mutable = "other" })
  assertEquals(foo, Foo(1, "someStr").apply { withCustomSetter = 1 })
  assertEquals(foo, Foo(1, "someStr").apply { jvmField = 101 })
  assertEquals(foo, Foo(1, "someStr").apply { lateinitField = "lateinit" })
  assertEquals(foo, Foo(1, "someStr").apply { setPrivateField(1) })
  val fooWithInitializedLazy = Foo(1, "someStr")
  assertEquals("test", fooWithInitializedLazy.lazyDelegated)
  assertEquals(foo, fooWithInitializedLazy)
}

fun testHashCode() {
  val foo = Foo(1, "someStr")

  assertEquals(foo.hashCode(), Foo(1, "someStr").hashCode())
  assertEquals(foo.hashCode(), Foo(1, "someStr", 10).hashCode())

  // Mutable constructor properties are considered for hashCode.
  assertNotEquals(foo.hashCode(), Foo(1, "someStr").apply { optional = 20 }.hashCode())

  // Non-constructor properties are not considered for hashCode.
  assertEquals(foo.hashCode(), Foo(1, "someStr").apply { mutable = "other" }.hashCode())
  assertEquals(foo.hashCode(), Foo(1, "someStr").apply { withCustomSetter = 1 }.hashCode())
  assertEquals(foo.hashCode(), Foo(1, "someStr").apply { jvmField = 101 }.hashCode())
  assertEquals(foo.hashCode(), Foo(1, "someStr").apply { lateinitField = "lateinit" }.hashCode())
  assertEquals(foo.hashCode(), Foo(1, "someStr").apply { setPrivateField(1) }.hashCode())
  val fooWithInitializedLazy = Foo(1, "someStr")
  assertEquals("test", fooWithInitializedLazy.lazyDelegated)
  assertEquals(foo.hashCode(), fooWithInitializedLazy.hashCode())
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
  assertEquals("Foo(bar=1, buzz=someStr, optional=10)", foo.toString())

  // Non-constructor properties are not considered for toString.
  foo.mutable = "other"
  foo.withCustomSetter = 50
  foo.jvmField = 101
  foo.lateinitField = "lateinit"
  foo.setPrivateField(777)
  assertEquals("other", foo.lazyDelegated)
  assertEquals("Foo(bar=1, buzz=someStr, optional=10)", foo.toString())
}

interface Named {
  val instance: Named
}

class NamedImpl() : Named {
  override val instance = this
}

data class DelegatingDataClass(val id: Int) : Named by NamedImpl()

fun testInterfaceDelegation() {
  assertNotEquals(DelegatingDataClass(1).instance, DelegatingDataClass(1).instance)
  assertEquals(DelegatingDataClass(1), DelegatingDataClass(1))
  assertEquals(DelegatingDataClass(1).hashCode(), DelegatingDataClass(1).hashCode())
  assertNotEquals(DelegatingDataClass(1).instance, DelegatingDataClass(2).instance)
  assertNotEquals(DelegatingDataClass(1), DelegatingDataClass(2))
}

data class SecondaryCtor(private val unreadField: Int) {
  var bar = 0

  constructor(foo: Int, buzz: Int) : this(100) {
    bar = foo + buzz
  }
}

fun testSecondaryConstructor() {
  assertNotEquals(SecondaryCtor(5), SecondaryCtor(6))

  assertEquals(SecondaryCtor(1, 2), SecondaryCtor(100, 200))
  assertEquals(SecondaryCtor(1, 2), SecondaryCtor(100))
  assertEquals(SecondaryCtor(1, 2).hashCode(), SecondaryCtor(100).hashCode())
  assertEquals("SecondaryCtor(unreadField=100)", SecondaryCtor(1, 2).toString())
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
  assertEquals(3, child.doSomething())

  val (a, b, c) = child
  assertEquals(1, a)
  assertEquals(2, b)
  assertEquals(-1, c)

  assertEquals(child, Child(1, 2))
  assertNotEquals(child, Child(2, 2))

  // Properties inherited from the superclass are not considered.
  val otherChild = Child(1, 2)
  otherChild.c = 3
  assertEquals(child, otherChild)
  assertEquals(child.hashCode(), otherChild.hashCode())
  assertEquals("Child(a=1, b=2)", otherChild.toString())
}

fun testLocalDataClassWithCaptures() {
  fun createLocal(captured: String, x: Int): Any {
    data class Local(val x: Int) {
      // Captures the local variable `captured`, which becomes a synthetic field.
      fun describe() = "$captured=$x"
    }
    return Local(x)
  }
  val local = createLocal("captured=1", 5)
  // Captured values are not considered for equals, hashCode, or toString.
  assertEquals(local, createLocal("captured=2", 5))
  assertEquals(local.hashCode(), createLocal("captured=2", 5).hashCode())
  assertNotEquals(local, createLocal("captured=1", 6))
  assertEquals("Local(x=5)", local.toString())
}

data class DataClassWithCustomObjectMethods(val x: Int) {
  override fun equals(other: Any?) = other is DataClassWithCustomObjectMethods

  override fun hashCode() = -999

  override fun toString() = "DataClassWithCustomObjectMethods"
}

open class BaseWithObjectOverrides {
  override fun equals(other: Any?) = false

  override fun hashCode() = -999

  final override fun toString() = "BaseOpenToString"
}

data class SubDataClassWithObjectOverrides(val x: Int) : BaseWithObjectOverrides()

fun testOverriddenObjectMethods() {
  val d = DataClassWithCustomObjectMethods(10)
  assertEquals(d, DataClassWithCustomObjectMethods(10))
  assertEquals(d, DataClassWithCustomObjectMethods(20))
  assertEquals(-999, DataClassWithCustomObjectMethods(10).hashCode())
  assertEquals("DataClassWithCustomObjectMethods", d.toString())

  val subDataClass: BaseWithObjectOverrides = SubDataClassWithObjectOverrides(10)
  // The superclass overrides equals() and hashCode() as open, so the data class generates
  // its own property-based implementations, which are used instead.
  assertEquals(subDataClass, SubDataClassWithObjectOverrides(10))
  assertNotEquals(subDataClass, SubDataClassWithObjectOverrides(20))
  assertEquals(subDataClass.hashCode(), SubDataClassWithObjectOverrides(10).hashCode())
  assertNotEquals(-999, subDataClass.hashCode())
  // The superclass overrides toString() as final, so the data class can't override it. No
  // toString() is generated and the superclass version is inherited.
  assertEquals("BaseOpenToString", subDataClass.toString())
}

data class FloatData(val f: Float, val d: Double) {}

fun testFloatingPoint() {
  val posZero = FloatData(0.0f, 0.0)
  val negZero = FloatData(-0.0f, -0.0)
  val nan = FloatData(Float.NaN, Double.NaN)

  assertEquals("FloatData(f=NaN, d=NaN)", nan.toString())

  if (isJavaScript()) {
    // In JS, NaN !== NaN and 0.0 === -0.0
    assertNotEquals(nan, FloatData(Float.NaN, Double.NaN))
    assertEquals(posZero, negZero)
    assertEquals("FloatData(f=0, d=0)", posZero.toString())
  } else {
    // In JVM, Float.compare treats NaN == NaN and 0.0 != -0.0
    assertEquals(nan, FloatData(Float.NaN, Double.NaN))
    assertNotEquals(posZero, negZero)
    assertEquals("FloatData(f=0.0, d=0.0)", posZero.toString())
  }
}

data class ArrayMembers(private val a: IntArray, private val b: Array<String>)

fun testArrayMembers() {
  val ints = intArrayOf(1, 2, 3)
  val strings = arrayOf("a", "b")
  val arrayMembers = ArrayMembers(ints, strings)

  assertEquals(arrayMembers, ArrayMembers(ints, strings))
  assertEquals(arrayMembers.hashCode(), ArrayMembers(ints, strings).hashCode())
  assertEquals("ArrayMembers(a=[1, 2, 3], b=[a, b])", arrayMembers.toString())

  // Array members use reference equality, but content-based hashCode and toString.
  val otherArrayMembers = ArrayMembers(intArrayOf(1, 2, 3), arrayOf("a", "b"))
  assertNotEquals(arrayMembers, otherArrayMembers)
  assertEquals(arrayMembers.hashCode(), otherArrayMembers.hashCode())
  assertEquals(arrayMembers.toString(), otherArrayMembers.toString())
}

interface JvmRecordInterface {
  val x: Int

  fun component2(): String
}

@kotlin.jvm.JvmRecord
data class JvmRecordDataClass(override val x: Int, val y: String) : JvmRecordInterface {
  override fun toString() = "Custom"
}

fun testJvmRecordDataClass() {
  val o = JvmRecordDataClass(1, "2")
  val i: JvmRecordInterface = o
  assertEquals(1, o.x)
  assertEquals(1, i.x)
  assertEquals("2", o.y)
  assertEquals("2", o.component2())
  assertEquals("2", i.component2())
  assertEquals("Custom", o.toString())
  assertEquals(o, JvmRecordDataClass(1, "2"))
  assertNotEquals(o, JvmRecordDataClass(1, "3"))
  assertEquals(o.hashCode(), JvmRecordDataClass(1, "2").hashCode())
}
