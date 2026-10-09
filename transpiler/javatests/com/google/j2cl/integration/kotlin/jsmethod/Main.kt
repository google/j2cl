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
@file:Suppress("KotlinConstantConditions")

package jsmethod

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertNull
import com.google.j2cl.integration.testing.Asserts.assertTrue
import java.io.Serializable
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

fun main(vararg args: String) {
  testJsMethodWithDifferentVisiblities()
  testInheritName()
  testLambdaImplementingJsMethod()
  testBoxedTypeSupertypes()
}

internal class NonPublicJsMethodClass {
  @JsMethod private fun foo(): String = "foo"

  internal fun fooProxy() = foo()

  @JsMethod internal fun bar(): String = "bar"
}

private fun testJsMethodWithDifferentVisiblities() {
  val instance = NonPublicJsMethodClass()
  assertEquals("foo", instance.fooProxy())
  assertEquals("bar", instance.bar())
  assertEquals("foo", callFoo(instance, null))
  assertEquals("bar", callBar(instance, null))
}

@JsMethod(namespace = "jsmethod.helper") public external fun callFoo(obj: Any?, param: Any?): Any?

@JsMethod(namespace = "jsmethod.helper") public external fun callBar(obj: Any?, param: Any?): Any?

internal open class ClassWithJsMethod {
  @JsMethod(name = "name") open fun className(): String = ClassWithJsMethod::class.java.name
}

internal class ClassWithJsMethodInheritingName : ClassWithJsMethod() {
  @JsMethod override fun className(): String = ClassWithJsMethodInheritingName::class.java.name
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
private interface HasName {
  fun name(): String
}

private fun callName(o: Any?): String = (o as HasName).name()

private fun testInheritName() {
  var o = ClassWithJsMethod()
  assertEquals(ClassWithJsMethod::class.java.name, o.className())
  assertEquals(ClassWithJsMethod::class.java.name, callName(o))

  o = ClassWithJsMethodInheritingName()
  assertEquals(ClassWithJsMethodInheritingName::class.java.name, o.className())
  assertEquals(ClassWithJsMethodInheritingName::class.java.name, callName(o))
}

internal fun interface FunctionalInterfaceWithJsMethod {
  @JsMethod fun greet(): String
}

private fun testLambdaImplementingJsMethod() {
  val f = FunctionalInterfaceWithJsMethod { "Hello" }
  assertEquals("Hello", f.greet())
}

// Use instance @JsMethods instead of static @JsMethods because static native @JsMethod imports
// are emitted as direct method references (emitAsMethodReference) when building the Wasm imports
// object, before the Wasm module is instantiated and populates its exported methods.
internal class BoxedSuperTypes {
  @JsMethod fun passThroughComparable(c: Comparable<*>?): Comparable<*>? = c

  @JsMethod fun passThroughCharSequence(cs: CharSequence?): CharSequence? = cs

  @JsMethod fun passThroughNumber(n: Number?): Number? = n

  @JsMethod fun passThroughSerializable(s: Serializable?): Serializable? = s
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
private interface NativeBoxedSuperTypes {
  fun <T> passThroughComparable(c: T?): Comparable<T>?

  fun passThroughCharSequence(cs: Any?): CharSequence?

  fun passThroughNumber(n: Any?): Number?

  fun passThroughSerializable(s: Any?): Serializable?
}

private fun testBoxedTypeSupertypes() {
  // Cast to a native interface to force the calls to cross the JS boundary.
  val tester = BoxedSuperTypes() as Any as NativeBoxedSuperTypes
  assertTrue(tester.passThroughComparable("hello")!!.compareTo("hello") == 0)
  assertTrue(tester.passThroughComparable(42.5)!!.compareTo(42.5) == 0)
  assertTrue(tester.passThroughComparable(true)!!.compareTo(true) == 0)
  assertTrue(tester.passThroughComparable(42L)!!.compareTo(42L) == 0)
  assertTrue(tester.passThroughComparable(42)!!.compareTo(42) == 0)
  assertNull(tester.passThroughComparable<String>(null))

  assertTrue(tester.passThroughCharSequence("hello")!![1] == 'e')
  assertTrue(tester.passThroughNumber(42.5)!!.toDouble() == 42.5)
  assertEquals(true, tester.passThroughSerializable(true))
}
