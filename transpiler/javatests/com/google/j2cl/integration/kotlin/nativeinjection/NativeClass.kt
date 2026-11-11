/*
 * Copyright 2023 Google Inc.
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
package nativeinjection

import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

@Suppress("unusable-by-js")
class NativeClass @JsConstructor constructor() {
  @JsMethod external fun nativeInstanceMethod(a: Int, b: Int): String

  @JsProperty external fun getNativeInstanceProperty(): String

  @JsMethod fun instanceMember(): Int = 10

  @JsMethod external fun callsInstanceMember(): Int

  @JsType(isNative = true, name = "*", namespace = JsPackage.GLOBAL) interface JsAny

  companion object {
    @JsMethod @JvmStatic external fun nativeStaticMethod(a: Int, b: Int): String

    @JsMethod @JvmStatic external fun nativeStaticMethodUsingLongAlias(): String

    @JsProperty @JvmStatic external fun getNativeStaticProperty(): Int

    @JsMethod @JvmStatic fun staticMember(value: Int): Int = 2 * value

    @JsMethod @JvmStatic external fun callsStaticMember(value: Int): Int

    // Implemented by the exposed overload below.
    @JsMethod @JvmStatic external fun implementedByExposedMember(value: Any): String

    @JsMethod
    @JvmStatic
    fun implementedByExposedMember(value: JsAny): String = "implementedByExposedMember:$value"

    @JsProperty @JvmStatic external fun getNativeInitializationCount(): Int

    @JsMethod @JvmStatic external fun callsConstructor(): Int
  }
}
