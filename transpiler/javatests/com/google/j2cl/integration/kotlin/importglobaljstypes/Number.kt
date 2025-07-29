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
  companion object {
    // import native js "Number" in a java class "Number".
    @JvmStatic
    @JsMethod(name = "Number.isInteger", namespace = GLOBAL)
    external fun `fun`(x: Double): Boolean

    @JvmStatic fun test(x: Double): Boolean = `fun`(x)

    @JvmStatic
    @JsProperty(name = "String.fromCharCode", namespace = GLOBAL)
    private external fun getFromCharCodeFunction(): NativeFunction

    @JvmStatic
    fun fromCharCode(array: IntArray): String =
      getFromCharCodeFunction().apply(null, array) as String
  }

  @JsType(isNative = true, namespace = GLOBAL, name = "Function")
  private interface NativeFunction {
    fun apply(thisContext: Any?, argsArray: IntArray): Any?
  }
}
