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

import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

class CastToNativeType {
  @JsType(isNative = true, namespace = "test.foo") class NativeJsType {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  class NativeObject<K, V> {}

  @Suppress("UNCHECKED_CAST")
  fun test() {
    val a: Any = NativeJsType()
    val aa = a as NativeJsType

    val b: Any = NativeJsType()
    val bb = b is NativeJsType

    val c: Any = NativeJsType()
    val cc = c as Array<NativeJsType>
    val d: Any = NativeJsType()
    var dd = (d as Array<*>).isArrayOf<NativeJsType>()

    val e: Any = NativeJsType()
    val ee = e as NativeObject<String, *>
    val f: Any = NativeJsType()
    val ff = f as NativeObject<String, Any?>
    val g: Any = NativeJsType()
    val gg = g is NativeObject<*, *>
    val h: Any = NativeJsType()
    val hh = h as Array<NativeObject<*, *>>
    val i: Any = NativeJsType()
    val ii = i as Array<NativeObject<String, Any?>>
    val j: Any = NativeJsType()
    val jj = (j as Array<*>).isArrayOf<NativeObject<*, *>>()
  }
}
