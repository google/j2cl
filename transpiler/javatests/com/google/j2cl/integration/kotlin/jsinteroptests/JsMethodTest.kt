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
package jsinteroptests

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsOptional
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsPackage.GLOBAL
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

/** Tests JsMethod functionality. */
object JsMethodTest {
  fun testAll() {
    testNativeJsMethod()
    testStaticNativeJsMethod()
    testStaticNativeJsPropertyGetter()
    testStaticNativeJsPropertySetter()
    testLambdaImplementingJsMethod()
    testLambdaRequiringJsMethodBridge()
    testJsOptionalJsVarargsLambda()
    // TODO(b/140309909): Either implement the feature and uncomment the test or ban it.
    // testPrivateJsMethodInInterface();
  }

  class MyObject {
    @JsProperty @JvmField var mine: Int = 0
  }

  private fun testNativeJsMethod() {
    val obj = MyObject()
    obj.mine = 0
    assertTrue(PropertyUtils.hasOwnPropertyMine(obj))
    assertFalse(PropertyUtils.hasOwnPropertyToString(obj))
  }

  @JsMethod(namespace = GLOBAL) @JvmStatic private external fun isFinite(d: Double): Boolean

  private fun testStaticNativeJsMethod() {
    assertFalse(isFinite(Double.POSITIVE_INFINITY))
    assertFalse(isFinite(Double.NEGATIVE_INFINITY))
    assertFalse(isFinite(Double.NaN))
    assertTrue(isFinite(0.0))
    assertTrue(isFinite(1.0))
  }

  @JsProperty(namespace = GLOBAL, name = "NaN") @JvmStatic private external fun getNaN(): Double

  @JsProperty(namespace = GLOBAL, name = "Infinity")
  @JvmStatic
  private external fun infinity(): Double

  private fun testStaticNativeJsPropertyGetter() {
    assertTrue(getNaN() != getNaN())
    assertTrue(infinity().isInfinite())
    assertTrue((-infinity()).isInfinite())
  }

  @JsProperty(namespace = GLOBAL, name = "window.jsInteropSecret")
  @JvmStatic
  private external fun setJsInteropSecret(magic: String)

  @JsProperty(namespace = GLOBAL, name = "window.jsInteropSecret")
  @JvmStatic
  private external fun getJsInteropSecret(): String

  private fun testStaticNativeJsPropertySetter() {
    setJsInteropSecret("very secret!")
    assertEquals("very secret!", getJsInteropSecret())
  }

  internal fun interface FunctionalInterfaceWithJsMethod {
    @JsMethod fun greet(): String
  }

  private fun testLambdaImplementingJsMethod() {
    val f = FunctionalInterfaceWithJsMethod { "Hello" }
    assertEquals("Hello", f.greet())
  }

  internal interface NullSupplier {
    fun get(): Any? = null
  }

  internal fun interface JsSupplier : NullSupplier {
    @JsMethod override fun get(): Any?
  }

  private fun testLambdaRequiringJsMethodBridge() {
    val aSupplier = JsSupplier { "Hello" }
    val aliasToSupplier: NullSupplier = aSupplier
    assertEquals("Hello", aSupplier.get())
    assertEquals("Hello", aliasToSupplier.get())
  }

  fun interface FunctionalInterfaceWithJsVarargsAndJsOptionalJsMethod<T : Number?> {
    @JsMethod fun sum(@JsOptional first: T?, vararg rest: T?): Double
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  internal interface NativeSum {
    @JsMethod fun sum(): Double
  }

  private fun testJsOptionalJsVarargsLambda() {
    val f =
      FunctionalInterfaceWithJsVarargsAndJsOptionalJsMethod<Double> { first, rest ->
        (first ?: 0.0) + rest.map { it as Double }.sum()
      }

    // Call lambda with no parameters.
    assertEquals(0, (f as NativeSum).sum())

    // Call lambda with wrong type of array. This is still OK since the method is a JsVarargs hence
    // the array is not passed directly in JavaScript but recreated by the JsVarargs prologue.
    @Suppress("UNCHECKED_CAST")
    val rawF = f as FunctionalInterfaceWithJsVarargsAndJsOptionalJsMethod<Number?>
    assertEquals(6, rawF.sum(1.0, *arrayOf<Number?>(2.0, 3.0)))
  }

  internal interface InterfaceWithPrivateJsMethod {
    @JsMethod private fun method(): String = "Private JsMethod"
  }

  @JsType(isNative = true, namespace = GLOBAL, name = "?")
  internal interface NativeInterface {
    fun method(): String
  }

  private fun testPrivateJsMethodInInterface() {
    val o = (object : InterfaceWithPrivateJsMethod {} as Any) as NativeInterface
    assertEquals("Private JsMethod", o.method())
  }
}
