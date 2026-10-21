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
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsOptional
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

/** Tests JsMethod functionality. */
object JsMethodTest {
  fun testAll() {
    testLambdaRequiringJsMethodBridge()
    testJsOptionalJsVarargsLambda()
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
}
