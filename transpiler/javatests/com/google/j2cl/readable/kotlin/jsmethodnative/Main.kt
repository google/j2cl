/*
 * Copyright 2026 Google Inc.
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
package jsmethodnative

import jsinterop.annotations.JsType

/** Tests imports of native methods taking a JsType declared in the application. */
class Main {
  // Non-native JsTypes need to be treated as opaque references when wasm jsinterop is disabled, and
  // as regular JsTypes when wasm jsinterop is enabled.
  @JsType class SomeJsType

  // Defined in native_sources/NativeClass.js.
  @JsType(isNative = true, namespace = "jsmethodnative", name = "NativeClass")
  class NativeClass {
    @Suppress("unusable-by-js") external fun instanceMethodReferencingNonNativeJsType(o: SomeJsType)

    companion object {
      @JvmStatic
      @Suppress("unusable-by-js")
      external fun staticMethodReferencingNonNativeJsType(o: SomeJsType)
    }
  }

  companion object {
    fun test() {
      val o = SomeJsType()
      NativeClass.staticMethodReferencingNonNativeJsType(o)
      NativeClass().instanceMethodReferencingNonNativeJsType(o)
    }
  }
}
