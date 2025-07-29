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
package importglobaljstypes

import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage.GLOBAL
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

/** Tests explicit import by namespaced JsMethod. */
class Number {

  /** Tests for generic native type. */
  @JsType(isNative = true, namespace = GLOBAL, name = "Array")
  private interface NativeArray<T> {
    @JsProperty fun getLength(): Int

    fun at(index: Int): T
  }

  companion object {
    // import native js "Number" in a java class "Number".
    @JsMethod(name = "Number.isInteger", namespace = GLOBAL)
    @JvmStatic
    external fun f(x: Double): Boolean

    @JvmStatic fun test(x: Double): Boolean = f(x)

    @JsMethod(name = "Array", namespace = GLOBAL)
    @JvmStatic
    private external fun <T> createArray(): NativeArray<T>

    @JvmStatic private fun getStringAt(index: Int): String = createArray<String>().at(index)

    @JvmStatic private fun getArrayLength(array: NativeArray<*>): Int = array.getLength()
  }

  @JsType(isNative = true, namespace = GLOBAL, name = "Object") interface MyLiteralType {}

  fun testJsDocForLiteralType(a: MyLiteralType): MyLiteralType = a
}
