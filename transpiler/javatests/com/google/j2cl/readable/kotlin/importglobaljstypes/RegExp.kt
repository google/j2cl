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
package importglobaljstypes

import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

internal class RegExp {

  companion object {
    @JvmStatic
    fun test() {
      val regExp = NativeRegExp("teststring")
      regExp.test("restString")
      val str = regExp.toString()
    }
  }

  @JsType(isNative = true, name = "RegExp", namespace = JsPackage.GLOBAL)
  class NativeRegExp(pattern: String) {
    external fun test(value: String): Boolean

    external override fun toString(): String
  }
}
