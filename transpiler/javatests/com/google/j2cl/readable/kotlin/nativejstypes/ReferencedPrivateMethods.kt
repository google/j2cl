/*
 * Copyright 2024 Google Inc.
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
package nativejstypes

import jsinterop.annotations.JsType
import kotlin.js.definedExternally

/** Tests for native types with private methods that are referenced. */
class ReferencedPrivateMethods {
  // In Kotlin, private members cannot be accessed from outside the class.
  // class Inner {
  //   fun getInstance(): Int {
  //     var t = NativeReferencedPrivateMethods()
  //     return t.x + t.s + t.getInstance() + t.getStatic()
  //   }
  // }

  // Class with private constructor that is referenced.
  @JsType(name = "NativeReferencedPrivateMethods", namespace = "nativejstypes", isNative = true)
  private class NativeReferencedPrivateMethods private constructor() {
    private var x: Int = definedExternally

    private external fun getInstance(): Int

    companion object {
      private var s: Int = definedExternally

      @JvmStatic private external fun getStatic(): Int

      fun getInstance(): Int {
        var t = NativeReferencedPrivateMethods()
        return t.x + s + t.getInstance() + getStatic()
      }
    }
  }
}
