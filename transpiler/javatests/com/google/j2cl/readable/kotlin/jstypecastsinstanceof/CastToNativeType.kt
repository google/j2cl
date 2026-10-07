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
package jstypecastsinstanceof

import javaemul.internal.annotations.UncheckedCast
import javaemul.internal.annotations.Wasm
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

class CastToNativeType {
  @JsType(isNative = true, namespace = "test.foo") open class NativeJsType {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  class NativeObject<K, V> {}

  @Suppress("UNCHECKED_CAST")
  fun test() {
    val a: Any = NativeJsType()
    val aa = a as NativeJsType

    val c: Any = NativeJsType()
    val cc = c as Array<NativeJsType>

    val e: Any = NativeJsType()
    val ee = e as NativeObject<String, *>
    val f: Any = NativeJsType()
    val ff = f as NativeObject<String, Any?>
    val h: Any = NativeJsType()
    val hh = h as Array<NativeObject<*, *>>
    val i: Any = NativeJsType()
    val ii = i as Array<NativeObject<String, Any?>>
  }

  private fun testUncheckedCast() {
    val a: NativeJsType = uncheckedCast(Any())
    val b: Array<NativeJsType> = uncheckedCast(Any())
    val c: NativeObject<String, *> = uncheckedCast(Any())
    val d: NativeObject<String, Any?> = uncheckedCast(Any())
    val e: Array<NativeObject<*, *>> = uncheckedCast(Any())
    val f: Array<NativeObject<String, Any?>> = uncheckedCast(Any())
  }

  @UncheckedCast @Suppress("UNCHECKED_CAST") private fun <T> uncheckedCast(o: Any?): T = o as T

  @Wasm("nop") // instanceof on native types not yet supported in Wasm.
  private fun testInstanceOfNative() {
    val b: Any = NativeJsType()
    val bb = b is NativeJsType
    val d: Any = NativeJsType()
    var dd = (d as Array<*>).isArrayOf<NativeJsType>()
    val g: Any = NativeJsType()
    val gg = g is NativeObject<*, *>
    val j: Any = NativeJsType()
    val jj = (j as Array<*>).isArrayOf<NativeObject<*, *>>()
  }

  private fun testInstanceOfNonNative() {
    val b = NativeJsType()
    val c = b is Any
    if (b is Comparable<*>) {
      val i: Comparable<*> = b
    }
  }
}
