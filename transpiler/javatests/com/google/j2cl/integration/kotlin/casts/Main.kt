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
package casts

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.TestUtils.isJvm
import java.io.Serializable
import javaemul.internal.annotations.Wasm
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

fun main(vararg unused: String) {
  testCasts_basics()
  testCasts_generics<Any, Number>()
  testCasts_typeVariableWithNativeBound<NativeMap<String, String>>()
  testCasts_parameterizedNativeType()
  testCasts_exceptionMessages()
  testCasts_exceptionMessages_jsType()
  testCasts_erasureCastOnThrow()
  testCasts_erasureCastOnNotNull()
  testCasts_erasureCastOnConversion()
  testCasts_notOptimizeable()
  testArrayCasts_basics()
  testArrayCasts_differentDimensions()
  testArrayCasts_sameDimensions()
  testArrayCasts_erasureCastsOnArrayAccess_fromArrayOfT()
  testArrayCasts_erasureCastsOnArrayAccess_fromT()
  testArrayCasts_boxedTypes()
  testDevirtualizedCasts_object()
  testDevirtualizedCasts_number()
  testDevirtualizedCasts_comparable()
  testDevirtualizedCasts_charSequence()
  testDevirtualizedCasts_void()
  testCasts_nullability()
  testCasts_implicitCasts()
  testSafeCastToTypeVariable()
  testPrecedence()
}

interface Interface

private fun testCasts_basics() {
  val o: Any? = null
  val s = o as String?
  val serializable: Serializable = object : Serializable {}
  val unusedSerializable = serializable as Serializable?
  assertThrowsClassCastException {
    val unused = serializable as RuntimeException?
  }
  val intf: Interface = object : Interface {}
  val unusedInterface = intf as Interface?

  assertThrowsClassCastException {
    val unused = intf as Serializable?
  }
}

@SuppressWarnings("SelfAssignment")
private fun testArrayCasts_basics() {
  // Cast null to Object[]
  var o: Any? = null as Array<Any>?

  // // Cast null to Object[][]
  o = null as Array<Any>?
  //
  // Cast JS "[]" to Object[]
  o = Array<Any?>(0) { null } // Actually emits as the JS array literal "[]".
  o = o as Array<Any?>?
  //
  // Cast JS "$Arrays.$init([], Object, 2))" to Object[][]
  o = Array(1) { Array<Any?>(0) { null } }
  o = o as Array<Array<Any?>>?
}

private fun testArrayCasts_sameDimensions() {
  var o: Any? = null

  val objects = Array<Any>(0) { Any() }
  val strings = Array<String>(0) { "" }
  val charSequences = Array<CharSequence>(0) { "" }

  o = objects as Array<Any>?
  o = strings as Array<Any>?
  o = strings as Array<String>?
  o = strings as Array<CharSequence>?
  o = charSequences as Array<Any>?
  o = charSequences as Array<CharSequence>?

  assertThrowsClassCastException(
    {
      val unused: Any? = objects as Array<String>?
    },
    Array<String>::class.java,
  )
  assertThrowsClassCastException(
    {
      val unused: Any? = objects as Array<CharSequence>?
    },
    Array<CharSequence>::class.java,
  )
  assertThrowsClassCastException(
    {
      val unused: Any? = charSequences as Array<String>?
    },
    Array<String>::class.java,
  )
}

private fun testArrayCasts_differentDimensions() {
  val `object`: Any = Array(10) { Array<Any>(10) { Any() } }

  // These are fine.
  val object1d = `object` as Array<Any>?
  val object2d = `object` as Array<Array<Any>>?

  // A 2d array cannot be cast to a 3d array.
  assertThrowsClassCastException(
    {
      val unused = object2d as Array<Array<Array<Any>>>?
    },
    Array<Array<Array<Any>>>::class.java,
  )

  // A non-array cannot be cast to an array.
  assertThrowsClassCastException(
    {
      val unused = Any() as Array<*>?
    },
    Array<Any>::class.java,
  )
}

private fun testArrayCasts_erasureCastsOnArrayAccess_fromArrayOfT() {
  // TODO(b/254148464): J2CL is more strict about erasure casts than JVM. Here JVM does not throw
  // assertThrows while J2CL does.
  if (isJvm()) {
    return
  }
  // Array of the right type.
  val stringArrayInArrayContainer = ArrayContainer<String>(Array<String?>(1) { null })
  val unusedString: String? = stringArrayInArrayContainer.data[0]
  val len = stringArrayInArrayContainer.data.size
  assertTrue(len == 1)

  // Array of the wrong type.
  val objectArrayInArrayContainer = ArrayContainer<String>(Array<Any?>(1) { null })
  assertThrowsClassCastException(
    {
      val unused: String = objectArrayInArrayContainer.data[0]
    },
    Array<String>::class.java,
  )
  // Make sure access to the length field performs the right cast. The length field
  // has special handling in CompilationUnitBuider.
  assertThrowsClassCastException(
    {
      val unused = objectArrayInArrayContainer.data.size
    },
    Array<String>::class.java,
  )

  // Not even an array.
  assertThrowsClassCastException(
    {
      val container = ArrayContainer<String>(Any())
    },
    Array<Any>::class.java,
  )
}

private class ArrayContainer<T> internal constructor(array: Any) {
  var data: Array<T>

  init {
    data = array as Array<T>
  }
}

private fun testArrayCasts_erasureCastsOnArrayAccess_fromT() {
  // TODO(b/254148464): J2CL is more strict about erasure casts than JVM. Here JVM does not throw
  // assertThrows while J2CL does.
  if (isJvm()) {
    return
  }

  // Array of the right type.
  val stringArrayInContainer = Container<Array<String>>(Array<String?>(1) { null })
  val unusedString = stringArrayInContainer.data[0]
  val len: Int = stringArrayInContainer.data.size
  assertTrue(len == 1)

  // Array of the wrong type.
  val objectArrayInContainer = Container<Array<String>>(Array<Any?>(1) { null })
  assertThrowsClassCastException(
    {
      val unused = objectArrayInContainer.data[0]
    },
    Array<String>::class.java,
  )
  assertThrowsClassCastException(
    {
      val unused: Int = objectArrayInContainer.data.size
    },
    Array<String>::class.java,
  )

  // Not even an array.
  val notAnArrayInContainer = Container<Array<String>>(Any())
  assertThrowsClassCastException(
    {
      val unused = notAnArrayInContainer.data[0]
    },
    Array<String>::class.java,
  )
  assertThrowsClassCastException(
    {
      val unused: Int = notAnArrayInContainer.data.size
    },
    Array<String>::class.java,
  )
}

private class Container<T> constructor(array: Any) {
  var data: T

  init {
    data = array as T
  }
}

private fun testArrayCasts_boxedTypes() {
  val b: Any = java.lang.Byte(1)
  val unusedB = b as Byte?
  var unusedN = b as Number?
  castToDoubleException(b)
  castToFloatException(b)
  castToIntegerException(b)
  castToLongException(b)
  castToShortException(b)
  castToCharacterException(b)
  castToBooleanException(b)
  val d: Any = 1.0
  val unusedD = d as Double?
  unusedN = d as Number?

  castToByteException(d)
  castToFloatException(d)
  castToIntegerException(d)
  castToLongException(d)
  castToShortException(d)
  castToCharacterException(d)
  castToBooleanException(d)
  val f: Any = 1.0f
  val unusedF = f as Float?
  unusedN = f as Number?

  castToByteException(f)
  castToDoubleException(f)
  castToIntegerException(f)
  castToLongException(f)
  castToShortException(f)
  castToCharacterException(f)
  castToBooleanException(f)
  val i: Any = 1
  val unusedI = i as Int?
  unusedN = i as Number?

  castToByteException(i)
  castToDoubleException(i)
  castToFloatException(i)
  castToLongException(i)
  castToShortException(i)
  castToCharacterException(i)
  castToBooleanException(i)
  val l: Any = 1L
  val unusedL = l as Long?
  unusedN = l as Number?

  castToByteException(l)
  castToDoubleException(l)
  castToFloatException(l)
  castToIntegerException(l)
  castToShortException(l)
  castToCharacterException(l)
  castToBooleanException(l)
  val s: Any = java.lang.Short(1)
  val unusedS = s as Short?
  unusedN = s as Number?

  castToByteException(s)
  castToDoubleException(s)
  castToFloatException(s)
  castToIntegerException(s)
  castToLongException(s)
  castToCharacterException(s)
  castToBooleanException(s)
  val c: Any = 'a'
  val unusedC = c as Char?

  castToByteException(c)
  castToDoubleException(c)
  castToFloatException(c)
  castToIntegerException(c)
  castToLongException(c)
  castToShortException(c)
  castToNumberException(c)
  castToBooleanException(c)
  val bool: Any = true
  val unusedBool = bool as Boolean?

  castToByteException(bool)
  castToDoubleException(bool)
  castToFloatException(bool)
  castToIntegerException(bool)
  castToLongException(bool)
  castToShortException(bool)
  castToNumberException(bool)
  castToCharacterException(bool)
  val sn: Any = SubNumber()
  unusedN = sn as Number?
}

private fun castToByteException(o: Any) {
  assertThrowsClassCastException {
    val b = o as Byte?
  }
}

private fun castToDoubleException(o: Any) {
  assertThrowsClassCastException {
    val d = o as Double?
  }
}

private fun castToFloatException(o: Any) {
  assertThrowsClassCastException {
    val f = o as Float?
  }
}

private fun castToIntegerException(o: Any) {
  assertThrowsClassCastException {
    val i = o as Int?
  }
}

private fun castToLongException(o: Any) {
  assertThrowsClassCastException {
    val l = o as Long?
  }
}

private fun castToShortException(o: Any) {
  assertThrowsClassCastException {
    val s = o as Short?
  }
}

private fun castToCharacterException(o: Any) {
  assertThrowsClassCastException {
    val c = o as Char?
  }
}

private fun castToBooleanException(o: Any) {
  assertThrowsClassCastException {
    val b = o as Boolean?
  }
}

private fun castToNumberException(o: Any) {
  assertThrowsClassCastException {
    val n = o as Number?
  }
}

private class SubNumber : Number() {
  override fun toByte(): Byte = 0

  override fun toChar(): Char = 'a'

  override fun toDouble(): Double = 0.0

  override fun toFloat(): Float = 0.0f

  override fun toInt(): Int = 0

  override fun toLong(): Long = 0L

  override fun toShort(): Short = 0
}

private fun testDevirtualizedCasts_object() {
  var unusedObject: Any? = null

  // All these casts should succeed.
  unusedObject = "" as Any?
  unusedObject = 0 as Any?
  unusedObject = false as Any?
  unusedObject = Array<Any?>(0) { null } as Any?
}

private fun testDevirtualizedCasts_number() {
  var unusedNumber: Number? = null

  // This casts should succeed.
  unusedNumber = 0.0 as Number?
}

private fun testDevirtualizedCasts_comparable() {
  var unusedComparable: Comparable<*>? = null

  // All these casts should succeed.
  unusedComparable = ""
  unusedComparable = 0
  unusedComparable = false
}

private fun testDevirtualizedCasts_charSequence() {
  var unusedCharSequence: CharSequence? = null

  // This casts should succeed.
  unusedCharSequence = ""
}

private fun testDevirtualizedCasts_void() {
  var unusedVoid: Void? = null

  // This casts should succeed.
  unusedVoid = null as Void?
}

private fun <T, E : Number?> testCasts_generics() {
  val o: Any = 1
  val e = o as E? // cast to type variable with bound, casting Integer instance to Number
  val t = o as T? // cast to type variable without bound, casting Integer instance to Object

  assertThrowsClassCastException {
    val error: Any = Error()
    val unused = error as E? // casting Error instance to Number, exception.
  }
  class Pameterized<T, E : Number?>

  val c: Any = Pameterized<Number, Number>()
  val cc = c as Pameterized<Error, Number>? // cast to parameterized type.

  val `is`: Array<Int?> = Array(1) { null }
  val os = Array<Any?>(1) { null }
  val es = `is` as Array<E?>?
  val ts = `is` as Array<T?>?
  assertThrowsClassCastException {
    val ees = os as Array<E?>?
  }
  val tts = os as Array<T?>?
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Map")
private class NativeMap<K, V>()

@Wasm("nop") // Casts to/from native types not yet supported in Wasm.
private fun <T : NativeMap<*, *>?> testCasts_typeVariableWithNativeBound() {
  // Casting Object[] to NativeMap[] is invalid on the JVM.
  if (isJvm()) {
    return
  }
  val o: Any = Array<Any?>(0) { null }
  val unusedArray = o as Array<T>? // cast to T[].

  assertThrowsClassCastException {
    val unused = o as T?
  }

  val o2: Any = NativeMap<Any?, Any?>()
  val unused = o2 as T?
}

@Wasm("nop") // Casts to/from native types not yet supported in Wasm.
private fun testCasts_parameterizedNativeType() {
  val a: Any = NativeMap<String, Any>()

  val e = a as NativeMap<*, *>?
  assertTrue(e === a)
  val f = a as NativeMap<String, Any>?
  assertTrue(f === a)
  assertTrue(a is NativeMap<*, *>)

  val tmp = arrayOf(e)
  val os: Any = tmp

  val g = os as Array<NativeMap<*, *>>
  assertTrue(g[0] === e)
  val h = os as Array<NativeMap<String, Any>>
  assertTrue(h[0] === e)
  assertTrue(os is Array<*>)
}

private class Foo

private class Bar

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String") private class Baz

@JsFunction
internal fun interface Qux {
  fun m(s: String?): String?
}

private fun testCasts_exceptionMessages() {
  val `object`: Any = Foo()
  assertThrowsClassCastException(
    {
      val bar = `object` as Bar?
    },
    Bar::class.java,
  )
  assertThrowsClassCastException(
    {
      val bars = `object` as Array<Bar>?
    },
    Array<Bar>::class.java,
  )

  assertThrowsClassCastException(
    {
      val string = `object` as String?
    },
    String::class.java,
  )
  assertThrowsClassCastException(
    {
      val aVoid = `object` as Void?
    },
    Void::class.java,
  )
}

@Wasm("nop") // Casts to/from native types not yet supported in Wasm.
private fun testCasts_exceptionMessages_jsType() {
  if (!isJvm()) {
    val `object`: Any = Foo()
    // Baz is a native JsType pointing to JavaScript string; the assertion does not make sense in
    // Java/JVM.
    assertThrowsClassCastException(
      {
        val baz = `object` as Baz?
      },
      "String",
    )

    // Qux is a native function; the assertion does not make sense in Java/JVM.
    assertThrowsClassCastException(
      {
        val qux = `object` as Qux?
      },
      "<native function>",
    )
  }
}

private fun testCasts_erasureCastOnThrow() {
  // TODO(b/254148464): J2CL is more strict about erasure casts than JVM. Here JVM does not throw
  // assertThrows while J2CL does.
  if (isJvm()) {
    return
  }
  assertThrowsClassCastException(
    { throw returnObjectAsT(RuntimeException()) },
    "java.lang.RuntimeException",
  )
}

private fun testCasts_erasureCastOnNotNull() {
  // TODO(b/254148464): J2CL is more strict about erasure casts than JVM. Here JVM does not throw
  //  assertThrows while J2CL does.
  if (isJvm()) {
    return
  }
  assertThrowsClassCastException(
    { returnObjectAsT<RuntimeException?>(RuntimeException())!! },
    "java.lang.RuntimeException",
  )
}

@Suppress("UNCHECKED_CAST") private fun <T> unsafeCastAs(o: Any?): T = o as T

private fun testCasts_erasureCastOnConversion() {
  assertThrowsClassCastException(
    {
      val i = returnObjectAsT(Integer(1))
    },
    "java.lang.Integer",
  )
}

private fun <T> returnObjectAsT(unused: T): T {
  return Any() as T
}

private var staticObject: Any = Foo()

private class AClass {
  var a = Holder()

  init {
    staticObject = Any()
  }
}

fun testCasts_notOptimizeable() {
  assertThrowsClassCastException {
    if (staticObject is Foo) {
      AClass().a.f = staticObject as Foo
    }
  }
  val h = Holder()
  assertThrowsClassCastException {
    if (h.reset().f is Foo) {
      val foo = h.reset().f as Foo?
    }
  }
}

private class Holder {
  var f: Any? = null

  fun reset(): Holder {
    f = if (f is Foo) Bar() else Foo()
    return this
  }
}

interface I

class A : I

class B : I

private fun testCasts_nullability() {
  val a = A()
  val nullB: B? = null
  val nonNullB: B? = B()

  val c = nullB as Foo?
  val d = nonNullB as B

  assertThrowsNullPointerException {
    val d = nullB as B
  }

  val d2 = nullB as? B
  assertTrue(d2 == null)

  assertThrowsClassCastException {
    val d = nonNullB as A?
  }
  val d3 = nonNullB as? A
  assertTrue(d3 == null)

  val e = a as I
  val f = a as I?

  val g = nonNullB as I
  val h = nonNullB as I?

  assertThrowsNullPointerException {
    val h = nullB as I
  }
  val h2 = nullB as? I
  assertTrue(d3 == null)

  val i = nullB as I?

  val j = nonNullB as? A
  assertTrue(j == null)
}

private fun testCasts_implicitCasts() {
  var i: Any? = 1
  if (i is Int) {
    // The usage of i in the arithmetic expression below has an implicit cast. In general
    // implicit casts are translated as a closure type cast, but in this case the cast
    // needs to be preserved so that the unboxing operation is performed.
    val result = i + 1
    assertEquals(2, result)
  }
}

interface I2<T>

open class C : I, I2<String>

class D : I2<String>

// Kotlin allows safe cast to type variable which uses instanceOf expression.
private fun testSafeCastToTypeVariable() {
  testSafeCastToTypeVariableWithPrimitiveUpperBound()
  testSafeCastToTypeWithUpperBound()
  testSafeCastToUnboundType()
  testSafeCastToIntersectionType()
}

private fun testSafeCastToTypeVariableWithPrimitiveUpperBound() {
  assertTrue(safeCastToTypeWithPrimitiveUpperBound<Int>(1) != null)
  assertTrue(safeCastToTypeWithPrimitiveUpperBound<Int>(Any()) == null)
}

private fun <T : Int> safeCastToTypeWithPrimitiveUpperBound(a: Any) = a as? T

private fun testSafeCastToTypeWithUpperBound() {
  assertTrue(safeCastToTypeWithUpperBound<D>(Any()) == null)
  assertTrue(safeCastToTypeWithUpperBound<D>(object : I2<Int> {}) != null)
  assertTrue(safeCastToTypeWithUpperBound<D>(D()) != null)
}

private fun <T : I2<String>> safeCastToTypeWithUpperBound(a: Any) = a as? T

private fun testSafeCastToUnboundType() {
  assertTrue(safeCastToUnboundType<A>(Any()) != null)
  assertTrue(safeCastToUnboundType<A>(object : I {}) != null)
  assertTrue(safeCastToUnboundType<A>(A()) != null)
}

private fun <T> safeCastToUnboundType(a: Any) = a as? T

private fun testSafeCastToIntersectionType() {
  assertTrue(safeCastToIntersectionType<C>(Any()) == null)

  assertTrue(safeCastToIntersectionType<C>(object : I2<String> {}) == null)
  assertTrue(safeCastToIntersectionType<C>(D()) == null)

  assertTrue(safeCastToIntersectionType<C>(object : I {}) != null)
  assertTrue(safeCastToIntersectionType<C>(A()) != null)

  assertTrue(safeCastToIntersectionType<C>(object : C() {}) != null)
  assertTrue(safeCastToIntersectionType<C>(C()) != null)
}

private fun <T> safeCastToIntersectionType(a: Any) where T : I, T : I2<String> = a as? T

private fun testPrecedence() {
  val foo: Any = "foo"
  val bar: Any = "bar"
  val notString = 123
  assertEquals("bar", (if (false) foo else bar) as String)
  assertEquals("foo123", ("foo" + notString) as String)
}
