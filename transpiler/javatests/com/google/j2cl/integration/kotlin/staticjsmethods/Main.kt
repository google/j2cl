/*
 * Copyright 2017 Google Inc.
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
package staticjsmethods

import com.google.j2cl.integration.testing.Asserts.assertTrue
import javaemul.internal.annotations.Wasm
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage.GLOBAL

object Main {
  @JsMethod(name = "fun") @JvmStatic fun f1(a: Int): Int = a + 11

  @JsMethod @JvmStatic fun f2(a: Int): Int = a + 22
}

fun testJsMethodsCalledByJava() {
  assertTrue(Main.f1(1) == 12)
  assertTrue(Main.f2(1) == 23)
}

@Wasm("nop") // .native.js not supported in Wasm.
fun testJsMethodsCalledByJS() {
  assertTrue(callF1(1) == 12)
  assertTrue(callF2(1) == 23)
}

fun testJsMethodsCalledByOtherClass() {
  assertTrue(OtherClass.callF1(1) == 12)
  assertTrue(OtherClass.callF2(1) == 23)
}

fun testNativeJsMethod() {
  assertTrue(floor(1.5) == 1)
  assertTrue(f3(-1) == 1)
  assertTrue(isFinite(1.0))
  assertTrue(max(2, 3) == 3)
  assertTrue(max(2, 3, 4) == 4)
  assertTrue(max(2.0, 3.0) == 3.0)
}

fun testDeepNamespaceNativeJsMethod() {
  assertTrue(fooBarAbs(-1.0) == 1.0)
}

fun main(vararg args: String) {
  testJsMethodsCalledByJava()
  testJsMethodsCalledByJS()
  testJsMethodsCalledByOtherClass()
  testNativeJsMethod()
  testDeepNamespaceNativeJsMethod()
}

@JsMethod @Wasm("nop") external fun callF1(a: Int): Int

@JsMethod @Wasm("nop") external fun callF2(a: Int): Int

@JsMethod(namespace = GLOBAL, name = "Math.floor") external fun floor(d: Double): Int

@JsMethod(namespace = GLOBAL, name = "Math.abs") external fun f3(d: Int): Int

@JsMethod(namespace = GLOBAL, name = "Math.max") external fun max(a: Int, b: Int): Int

@JsMethod(namespace = GLOBAL, name = "Math.max") external fun max(a: Int, b: Int, c: Int): Int

@JsMethod(namespace = GLOBAL, name = "Math.max") external fun max(a: Double, b: Double): Double

@JsMethod(namespace = GLOBAL, name = "isFinite") external fun isFinite(d: Double): Boolean

@JsMethod(namespace = "foo.Bar", name = "abs") external fun fooBarAbs(d: Double): Double
