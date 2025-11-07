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
package instanceofs

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue
import java.io.Serializable
import java.lang.Cloneable

/** Test instanceof array. */
fun main(vararg unused: String) {
  testInstanceOf_class()
  testInstanceOf_interface()
  testInstanceOf_array()
  testInstanceOf_boxedTypes()
  testInstanceOf_unboxedTypes()
  testInstanceOf_string()
  testInstanceOf_sideEffects()
  testInstanceOf_markerInterfaces()
}

private fun testInstanceOf_class() {
  class SomeClass

  val obj: Any = SomeClass()
  assertTrue(obj is SomeClass)
  assertTrue(obj is Any)
  assertTrue(obj !is String)
  assertTrue("A String Literal" is String)
  assertTrue(null !is Any)
}

private fun testInstanceOf_sideEffects() {
  counter = 0
  assertTrue(incrementCounter() is Any)
  assertEquals(1, counter)
  assertFalse(incrementCounter() is Number)
  assertEquals(2, counter)
  assertFalse(incrementCounter() is Serializable)
  assertEquals(3, counter)
}

private var counter = 0

private fun incrementCounter(): Any? {
  counter++
  return Any()
}

private fun testInstanceOf_interface() {
  var obj: Any = Implementor()
  assertTrue(obj is ParentInterface)
  assertTrue(obj is ChildInterface)
  assertTrue(obj is GenericInterface<*>)
  assertFalse(obj is Serializable)
  assertFalse(obj is Cloneable)

  // Serializable and Cloneable have custom isInstance implementations; make sure those
  // still behave correctly when classes implement the interface.
  obj = object : Serializable {}
  assertTrue(obj is Serializable)
  obj = object : Cloneable {}
  assertTrue(obj is Cloneable)
}

private fun testInstanceOf_array() {
  val obj = Any()
  assertTrue(obj is Any)
  assertTrue(obj !is Array<*>)
  assertTrue(!(obj is Array<*> && obj.isArrayOf<Array<Any>>()))
  assertTrue(!(obj is Array<*> && obj.isArrayOf<String>()))
  assertTrue(!(obj is Array<*> && obj.isArrayOf<Array<String>>()))
  assertTrue(obj !is IntArray)
  assertTrue(obj !is Comparable<*>)
  assertTrue(obj !is Serializable)
  assertTrue(obj !is Cloneable)

  val objects1d: Any = arrayOfNulls<Any>(1)
  assertTrue(objects1d is Any)
  assertTrue(objects1d is Array<*> && objects1d.isArrayOf<Any>())
  assertTrue(!(objects1d is Array<*> && objects1d.isArrayOf<Array<Any>>()))
  assertTrue(!(objects1d is Array<*> && objects1d.isArrayOf<String>()))
  assertTrue(!(objects1d is Array<*> && objects1d.isArrayOf<Array<String>>()))
  assertTrue(objects1d !is IntArray)
  assertTrue(objects1d !is Comparable<*>)
  assertTrue(objects1d is Serializable)
  assertTrue(objects1d is Cloneable)

  val strings1d: Any = arrayOfNulls<String>(1)
  assertTrue(strings1d is Any)
  assertTrue(strings1d is Array<*> && strings1d.isArrayOf<Any>())
  assertTrue(!(strings1d is Array<*> && strings1d.isArrayOf<Array<Any>>()))
  assertTrue(strings1d is Array<*> && strings1d.isArrayOf<String>())
  assertTrue(!(strings1d is Array<*> && strings1d.isArrayOf<Array<String>>()))
  assertTrue(strings1d !is IntArray)
  assertTrue(strings1d !is Comparable<*>)
  assertTrue(strings1d is Serializable)
  assertTrue(strings1d is Cloneable)

  val objects2d: Any = arrayOf(arrayOfNulls<Any>(1))
  assertTrue(objects2d is Any)
  assertTrue(objects2d is Array<*> && objects2d.isArrayOf<Any>())
  assertTrue(objects2d is Array<*> && objects2d.isArrayOf<Array<Any>>())
  assertTrue(!(objects2d is Array<*> && objects2d.isArrayOf<String>()))
  assertTrue(!(objects2d is Array<*> && objects2d.isArrayOf<Array<String>>()))
  assertTrue(objects2d !is IntArray)
  assertTrue(objects2d !is Comparable<*>)
  assertTrue(objects2d is Serializable)
  assertTrue(objects2d is Cloneable)

  val strings2d: Any = arrayOf(arrayOfNulls<String>(1))
  assertTrue(strings2d is Any)
  assertTrue(strings2d is Array<*> && strings2d.isArrayOf<Any>())
  assertTrue(strings2d is Array<*> && strings2d.isArrayOf<Array<Any>>())
  assertTrue(!(strings2d is Array<*> && strings2d.isArrayOf<String>()))
  assertTrue(strings2d is Array<*> && strings2d.isArrayOf<Array<String>>())
  assertTrue(strings2d !is IntArray)
  assertTrue(strings2d !is Comparable<*>)
  assertTrue(strings2d is Serializable)
  assertTrue(strings2d is Cloneable)

  val ints1d: Any = IntArray(1)
  assertTrue(ints1d is Any)
  assertTrue(!(ints1d is Array<*> && ints1d.isArrayOf<Any>()))
  assertTrue(!(ints1d is Array<*> && ints1d.isArrayOf<Array<Any>>()))
  assertTrue(!(ints1d is Array<*> && ints1d.isArrayOf<String>()))
  assertTrue(!(ints1d is Array<*> && ints1d.isArrayOf<Array<String>>()))
  assertTrue(ints1d is IntArray)
  assertTrue(ints1d !is Comparable<*>)
  assertTrue(ints1d is Serializable)
  assertTrue(ints1d is Cloneable)

  val ints2d: Any = arrayOfNulls<IntArray>(1)
  assertTrue(ints2d is Any)
  assertTrue(ints2d is Array<*> && ints2d.isArrayOf<Any>())
  assertTrue(!(ints2d is Array<*> && ints2d.isArrayOf<Array<Any>>()))
  assertTrue(!(ints2d is Array<*> && ints2d.isArrayOf<String>()))
  assertTrue(!(ints2d is Array<*> && ints2d.isArrayOf<Array<String>>()))
  assertTrue(ints2d !is IntArray)
  assertTrue(ints2d is Array<*> && ints2d.isArrayOf<IntArray>())
  assertTrue(ints2d !is Comparable<*>)
  assertTrue(ints2d is Serializable)
  assertTrue(ints2d is Cloneable)

  val intsLiteral: Any = intArrayOf(1, 2, 3)
  assertTrue(intsLiteral is Any)
  assertTrue(!(intsLiteral is Array<*> && intsLiteral.isArrayOf<Any>()))
  assertTrue(!(intsLiteral is Array<*> && intsLiteral.isArrayOf<Array<Any>>()))
  assertTrue(!(intsLiteral is Array<*> && intsLiteral.isArrayOf<String>()))
  assertTrue(!(intsLiteral is Array<*> && intsLiteral.isArrayOf<Array<String>>()))
  assertTrue(intsLiteral is IntArray)
  assertTrue(intsLiteral !is Comparable<*>)
  assertTrue(intsLiteral is Serializable)
  assertTrue(intsLiteral is Cloneable)
}

private fun testInstanceOf_boxedTypes() {
  var o: Any? = 1.toByte()
  assertTrue(o is Byte?)
  assertTrue(o !is Double?)
  assertTrue(o !is Float?)
  assertTrue(o !is Int?)
  assertTrue(o !is Long?)
  assertTrue(o !is Short?)
  assertTrue(o is Number?)
  assertTrue(o !is Char?)
  assertTrue(o !is Boolean?)
  assertTrue(o is Comparable<*>)
  assertTrue(o is Serializable)
  assertTrue(o !is Cloneable)
  o = 1.0
  assertTrue(o !is Byte?)
  assertTrue(o is Double?)
  assertTrue(o !is Float?)
  assertTrue(o !is Int?)
  assertTrue(o !is Long?)
  assertTrue(o !is Short?)
  assertTrue(o is Number?)
  assertTrue(o !is Char?)
  assertTrue(o !is Boolean?)
  assertTrue(o is Comparable<*>)
  assertTrue(o is Serializable)
  assertTrue(o !is Cloneable)
  o = 1.0f
  assertTrue(o !is Byte?)
  assertTrue(o !is Double?)
  assertTrue(o is Float?)
  assertTrue(o !is Int?)
  assertTrue(o !is Long?)
  assertTrue(o !is Short?)
  assertTrue(o is Number)
  assertTrue(o !is Char?)
  assertTrue(o !is Boolean?)
  assertTrue(o is Comparable<*>)
  assertTrue(o is Serializable)
  assertTrue(o !is Cloneable)
  o = 1
  assertTrue(o !is Byte?)
  assertTrue(o !is Double?)
  assertTrue(o !is Float?)
  assertTrue(o is Int?)
  assertTrue(o !is Long?)
  assertTrue(o !is Short?)
  assertTrue(o is Number)
  assertTrue(o !is Char?)
  assertTrue(o !is Boolean?)
  assertTrue(o is Comparable<*>)
  assertTrue(o is Serializable)
  assertTrue(o !is Cloneable)
  o = 1L
  assertTrue(o !is Byte?)
  assertTrue(o !is Double?)
  assertTrue(o !is Float?)
  assertTrue(o !is Int?)
  assertTrue(o is Long?)
  assertTrue(o !is Short?)
  assertTrue(o is Number)
  assertTrue(o !is Char?)
  assertTrue(o !is Boolean?)
  assertTrue(o is Comparable<*>)
  assertTrue(o is Serializable)
  assertTrue(o !is Cloneable)
  o = 1.toShort()
  assertTrue(o !is Byte?)
  assertTrue(o !is Double?)
  assertTrue(o !is Float?)
  assertTrue(o !is Int?)
  assertTrue(o !is Long?)
  assertTrue(o is Short?)
  assertTrue(o is Number)
  assertTrue(o !is Char?)
  assertTrue(o !is Boolean?)
  assertTrue(o is Comparable<*>)
  assertTrue(o is Serializable)
  assertTrue(o !is Cloneable)
  o = 'a'
  assertTrue(o !is Byte?)
  assertTrue(o !is Double?)
  assertTrue(o !is Float?)
  assertTrue(o !is Int?)
  assertTrue(o !is Long?)
  assertTrue(o !is Short?)
  assertTrue(o !is Number)
  assertTrue(o is Char?)
  assertTrue(o !is Boolean?)
  assertTrue(o is Comparable<*>)
  assertTrue(o is Serializable)
  assertTrue(o !is Cloneable)
  o = true
  assertTrue(o !is Byte?)
  assertTrue(o !is Double?)
  assertTrue(o !is Float?)
  assertTrue(o !is Int?)
  assertTrue(o !is Long?)
  assertTrue(o !is Short?)
  assertTrue(o !is Number)
  assertTrue(o !is Char?)
  assertTrue(o is Boolean?)
  assertTrue(o is Comparable<*>)
  assertTrue(o is Serializable)
  assertTrue(o !is Cloneable)
  o = NumberSubclass()
  assertTrue(o is NumberSubclass)
  assertTrue(o is Number)
  assertTrue(o is Serializable)
  assertTrue(o !is Cloneable)
  assertTrue(Any() !is Void)
  assertTrue(null !is Void)
  assertTrue(null !is Cloneable)
  assertTrue(null !is Serializable)
  o = null
  assertTrue(o is Byte?)
  assertTrue(o is Double?)
  assertTrue(o is Float?)
  assertTrue(o is Int?)
  assertTrue(o is Long?)
  assertTrue(o is Short?)
  assertTrue(o is Number?)
  assertTrue(o is Char?)
  assertTrue(o is Boolean?)
  assertTrue(o is Comparable<*>?)
  assertTrue(o is Serializable?)
  assertTrue(o is Cloneable?)
  assertTrue(o !is Byte)
  assertTrue(o !is Double)
  assertTrue(o !is Float)
  assertTrue(o !is Int)
  assertTrue(o !is Long)
  assertTrue(o !is Short)
  assertTrue(o !is Number)
  assertTrue(o !is Char)
  assertTrue(o !is Boolean)
  assertTrue(o !is Comparable<*>)
  assertTrue(o !is Serializable)
  assertTrue(o !is Cloneable)
}

private fun testInstanceOf_unboxedTypes() {
  var b = 1.toByte()
  assertTrue(b is Byte?)
  assertTrue(b is Byte)
  assertTrue(b is Number?)
  assertTrue(b is Number)
  assertTrue(b is Comparable<*>?)
  assertTrue(b is Comparable<*>)
  assertTrue(b is Serializable?)
  assertTrue(b is Serializable)
  val d = 1.0
  assertTrue(d is Double)
  assertTrue(d is Double?)
  assertTrue(d is Number?)
  assertTrue(d is Number)
  assertTrue(d is Comparable<*>?)
  assertTrue(d is Comparable<*>)
  assertTrue(d is Serializable?)
  assertTrue(d is Serializable)
  val f = 1.0f
  assertTrue(f is Float?)
  assertTrue(f is Float)
  assertTrue(f is Number?)
  assertTrue(f is Number)
  assertTrue(f is Comparable<*>?)
  assertTrue(f is Comparable<*>)
  assertTrue(f is Serializable?)
  assertTrue(f is Serializable)
  val i = 1
  assertTrue(i is Int?)
  assertTrue(i is Int)
  assertTrue(i is Number?)
  assertTrue(i is Number)
  assertTrue(i is Comparable<*>?)
  assertTrue(i is Comparable<*>)
  assertTrue(i is Serializable?)
  assertTrue(i is Serializable)
  val l = 1L
  assertTrue(l is Long?)
  assertTrue(l is Long)
  assertTrue(l is Number?)
  assertTrue(l is Number)
  assertTrue(l is Comparable<*>?)
  assertTrue(l is Comparable<*>)
  assertTrue(l is Serializable?)
  assertTrue(l is Serializable)
  val s = 1.toShort()
  assertTrue(s is Short?)
  assertTrue(s is Short)
  assertTrue(s is Number?)
  assertTrue(s is Number)
  assertTrue(s is Comparable<*>?)
  assertTrue(s is Comparable<*>)
  assertTrue(s is Serializable?)
  assertTrue(s is Serializable)
  val c = 'a'
  assertTrue(c is Char?)
  assertTrue(c is Char)
  assertTrue(c is Comparable<*>?)
  assertTrue(c is Comparable<*>)
  assertTrue(c is Serializable?)
  assertTrue(c is Serializable)
  val bool = true
  assertTrue(bool is Boolean?)
  assertTrue(bool is Boolean)
  assertTrue(bool is Comparable<*>?)
  assertTrue(bool is Comparable<*>)
  assertTrue(bool is Serializable?)
  assertTrue(bool is Serializable)

  // Ensure we persist side-effects
  var num = 0
  assertTrue(++num is Int)
  assertTrue(num == 1)
}

private fun testInstanceOf_string() {
  val s: Any = "A string"
  assertTrue(s !is Byte?)
  assertTrue(s !is Double?)
  assertTrue(s !is Float?)
  assertTrue(s !is Int?)
  assertTrue(s !is Long?)
  assertTrue(s !is Short?)
  assertTrue(s !is Number)
  assertTrue(s !is Char?)
  assertTrue(s !is Boolean?)
  assertTrue(s is String)
  assertTrue(s is Comparable<*>)
  assertTrue(s is Serializable)
  assertTrue(s !is Cloneable)
}

private fun testInstanceOf_markerInterfaces() {
  class A : MarkerA
  class B : MarkerB

  var a: Any = A()
  assertTrue(a is MarkerA)
  assertFalse(a is MarkerB)

  var b: Any = B()
  assertTrue(b is MarkerB)
  assertFalse(b is MarkerA)
}

internal interface MarkerA

internal interface MarkerB

interface ParentInterface {
  fun dummy() {}
}

interface ChildInterface : ParentInterface {
  fun dummy(l: Long) {}
}

interface GenericInterface<T> {
  fun dummy(i: Int) {}
}

private class Implementor : ChildInterface, GenericInterface<String?>

private class NumberSubclass : Number() {
  override fun toByte(): Byte = 0

  override fun toChar(): Char = 'a'

  override fun toDouble(): Double = 0.0

  override fun toFloat(): Float = 0.0f

  override fun toInt(): Int = 0

  override fun toLong(): Long = 0L

  override fun toShort(): Short = 0
}
