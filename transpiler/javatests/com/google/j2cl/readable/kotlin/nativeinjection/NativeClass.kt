/*
 * Copyright 2023 Google Inc.
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
package nativeinjection

import jsinterop.annotations.JsMethod

/**
 * Kotlin only matches qualified name for native file due to package structure. The non-qualified
 * scenarios are converted qualified hence not tested.
 */
class NativeClass {
  @JsMethod external fun nativeInstanceMethod(): String

  class InnerClass {
    @JsMethod external fun nativeInstanceMethod(): String
  }

  companion object {
    @JvmStatic @JsMethod external fun nativeStaticMethod(): NativeClass

    @JvmStatic @JsMethod internal external fun notPublicNativeStaticMethod(): NativeClass
  }
}
