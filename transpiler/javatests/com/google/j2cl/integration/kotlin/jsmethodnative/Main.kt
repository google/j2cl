/*
 * Copyright 2015 Google Inc.
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
package jsmethodnative

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertNull
import com.google.j2cl.integration.testing.Asserts.assertSame
import com.google.j2cl.integration.testing.Asserts.assertTrue
import java.io.Serializable
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage.GLOBAL
import jsinterop.annotations.JsType

fun main(vararg args: String) {
  testNativeJsMethodOnNonNativeType()
  testStaticNativeJsMethod()
  testForwardingMethodsOnNativeClasses()
  testCallNamespaceAsFunction()
  testBoxedTypeSupertypes()
}

private class NonNativeTypeWithNativeMethods {
  @JsMethod(name = "toString") external fun callToString(): String

  @JsMethod external fun valueOf(): Any
}

private fun testNativeJsMethodOnNonNativeType() {
  val obj = NonNativeTypeWithNativeMethods()
  assertEquals(obj.toString(), obj.callToString())
  assertSame(obj, obj.valueOf())
}

@JsMethod(namespace = GLOBAL) private external fun isFinite(d: Double): Boolean

private fun testStaticNativeJsMethod() {
  assertFalse(isFinite(Double.POSITIVE_INFINITY))
  assertFalse(isFinite(Double.NEGATIVE_INFINITY))
  assertFalse(isFinite(Double.NaN))
  assertTrue(isFinite(0.0))
  assertTrue(isFinite(1.0))
}

@JsType(isNative = true, namespace = GLOBAL, name = "?")
internal interface NativeInterface {
  fun add(element: String?)
}

@JsType(isNative = true, namespace = "jsmethodnative", name = "NativeClass")
internal open class NativeSuperClass {
  external fun add(element: String?)

  external fun remove(element: String?): Boolean
}

@JsType(isNative = true, namespace = "jsmethodnative", name = "NativeClass")
internal class NativeSubClassAccidentalOverride : NativeSuperClass(), NativeInterface

private fun testForwardingMethodsOnNativeClasses() {
  val subClass = NativeSubClassAccidentalOverride()
  subClass.add("Hi")
  assertTrue(subClass.remove("Hi"))
  assertFalse(subClass.remove("Hi"))
}

@JsMethod(namespace = "jsmethodnative.FunctionNamespace", name = "")
private external fun callFunctionNamespace(input: String): String

private fun testCallNamespaceAsFunction() {
  assertEquals("foobar", callFunctionNamespace("foo"))
}

@JsMethod(namespace = "jsmethodnative.helper", name = "passThrough")
private external fun <T> passThroughComparable(c: Comparable<T>?): Comparable<T>?

@JsMethod(namespace = "jsmethodnative.helper", name = "passThrough")
private external fun passThroughCharSequence(cs: CharSequence?): CharSequence?

@JsMethod(namespace = "jsmethodnative.helper", name = "passThrough")
private external fun passThroughNumber(n: Number?): Number?

@JsMethod(namespace = "jsmethodnative.helper", name = "passThrough")
private external fun passThroughSerializable(s: Serializable?): Serializable?

private fun testBoxedTypeSupertypes() {
  assertNull(passThroughComparable<String>(null))
  assertTrue(passThroughComparable("hello")!!.compareTo("hello") == 0)
  assertTrue(passThroughComparable(42.5)!!.compareTo(42.5) == 0)
  assertTrue(passThroughComparable(true)!!.compareTo(true) == 0)
  assertTrue(passThroughComparable(42L)!!.compareTo(42L) == 0)
  assertTrue(passThroughComparable(42)!!.compareTo(42) == 0)
  val customComparable =
    object : Comparable<Any> {
      override fun compareTo(other: Any): Int = 0
    }
  assertTrue(passThroughComparable(customComparable)!!.compareTo(customComparable) == 0)

  assertNull(passThroughCharSequence(null))
  assertTrue(passThroughCharSequence("hello")!![1] == 'e')
  val customCharSequence: CharSequence = StringBuilder("world")
  assertTrue(passThroughCharSequence(customCharSequence)!![1] == 'o')

  assertNull(passThroughNumber(null))
  assertTrue(passThroughNumber(42.5)!!.toDouble() == 42.5)
  assertTrue(passThroughNumber(42L)!!.toLong() == 42L)
  assertTrue(passThroughNumber(42)!!.toInt() == 42)

  assertNull(passThroughSerializable(null))
  assertEquals("hello", passThroughSerializable("hello"))
  assertEquals(42.5, passThroughSerializable(42.5))
  assertEquals(true, passThroughSerializable(true))
  assertEquals(42L, passThroughSerializable(42L))
  val customSerializable = object : Serializable {}
  assertSame(customSerializable, passThroughSerializable(customSerializable))
}
