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

/** Native JsType with a private members. */
class NonreferencedMethods {
  // Public class with private members that are not referenced.
  @JsType(name = "NativeNonreferencedMethods", namespace = "nativejstypes", isNative = true)
  class NativePrivateMembersOnPublicClass private constructor(x: Int) {
    private var x: Int = definedExternally

    private external fun getInstance1(): Int

    companion object {
      private var s: Int = definedExternally

      @JvmStatic private external fun getStatic(): Int
    }
  }

  // Private class with visible members that are not referenced.
  @JsType(name = "NativeNonreferencedMethods", namespace = "nativejstypes", isNative = true)
  private class NativeMembersOnPrivateClass(x: Int) {
    // Not referenced polymorphic.
    external fun getInstance2(): Int

    companion object {
      var s: Int = definedExternally

      @JvmStatic external fun getStatic(): Int
    }
  }
}
