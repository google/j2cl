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
package jsfunction

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsOverlay

fun main(vararg unused: String) {
  testJsFunction()
  testParameterizedJsFunctionMethod()
}

@JsFunction
internal fun interface Function {
  fun call(): Boolean

  @JsOverlay
  fun overlay(): Int {
    return f + if (call()) 1 else 2
  }

  companion object {
    @JvmField // TODO(b/570022569): remove when JsInterop support Kotlin construct.
    @JsOverlay
    val f = 1
  }
}

@JsFunction
internal fun interface FunctionWithStaticOverlay {
  fun call(): Boolean

  companion object {
    @JsOverlay
    @JvmStatic // TODO(b/570022569): remove when JsInterop support Kotlin construct.
    fun overlay(): Int {
      return 4
    }
  }
}

@JsFunction
internal fun interface FunctionWithStaticField {
  fun call(): Boolean

  // TODO(b/303321920): Remove when JsFunction with only static field produces an Overlay class.
  @JsOverlay
  fun overlay(): Int {
    return 0
  }

  companion object {
    @JvmField // TODO(b/570022569): remove when JsInterop support Kotlin construct.
    @JsOverlay
    val f = 1
  }
}

private fun testJsFunction() {
  assertTrue(Function { true }.overlay() == 2)
  assertTrue(Function { false }.overlay() == 3)
  assertTrue(FunctionWithStaticOverlay.overlay() == 4)
  assertTrue(FunctionWithStaticField.f == 1)
}

private fun testSpecializedJsFunction() {
  val stringConsumer = Consumer { s: String -> s + "!" }
  val anyConsumer: Consumer<Any> = stringConsumer as Consumer<Any>
  assertThrowsClassCastException({ anyConsumer.accept(Any()) }, String::class.java)
}

@JsFunction
internal fun interface Consumer<T> {
  fun accept(t: T)
}

@JsFunction
internal fun interface ParameterizedInterface<T> {
  fun f(t: T?): T?
}

fun <T> identity(t: T?): T? {
  return t
}

fun <T> nullFn(t: T?): T? {
  return null
}

private fun testParameterizedJsFunctionMethod() {
  open class A {
    open fun m(): String {
      return "HelloA"
    }
  }

  class B : A() {
    override fun m(): String {
      return "HelloB"
    }
  }

  var FALSE = false

  val parameterInterfaceFn: ParameterizedInterface<B>
  // Use a mutable global to make the kotlin frontend not know which method is actually passed so
  // that so the construct is actually materialized in the AST.
  if (FALSE) {
    parameterInterfaceFn = ParameterizedInterface<B>(::nullFn)
  } else {
    parameterInterfaceFn = ParameterizedInterface<B>(::identity)
  }
  A()
  assertEquals("HelloB", parameterInterfaceFn.f(B())!!.m())
}
