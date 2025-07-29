/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
package jstype

import javaemul.internal.annotations.Wasm
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

@JsType
open class SomeJsType<T>(
  var publicField: Int = 0,
  private var privateField: Int = 0,
  internal var packageField: Int = 0,
  protected var protectedField: Int = 0,
) {

  fun publicMethod() {}

  private fun privateMethod() {}

  internal fun packageMethod() {}

  protected fun protectedMethod() {}

  fun useFieldsAndMethods() {
    val value = publicField + privateField + packageField + protectedField
    publicMethod()
    privateMethod()
    packageMethod()
    protectedMethod()
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "*") interface Star

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?") interface Wildcard

  @Wasm("nop") // TODO(b/262009761): Casts between Object and JsTypes not supported in Wasm.
  private fun testStarAndWildCard(s: Star, w: Wildcard): Wildcard {
    val obj = Object()

    val star = (3.0 as Any) as Star
    return star as Wildcard
  }
}

@JsType
open class SomeFields<T>() {
  var publicField: Int = 0
  private var privateField: Int = 0
  internal var packageField: Int = 0
  protected var protectedField: Int = 0
}

@JsType
open class SomeJvmFields<T>() {
  @JvmField var publicField: Int = 0
  private var privateField: Int = 0
  @JvmField internal var packageField: Int = 0
  @JvmField protected var protectedField: Int = 0
}

private fun testAccessingSeparatelyCompiledJsProperty() {
  val a = SeparatelyCompiledJsType("foo").foo
  val b = SeparatelyCompiledJsType("noBackingField").noBackingField
  val c = SeparatelyCompiledClass().f
}
