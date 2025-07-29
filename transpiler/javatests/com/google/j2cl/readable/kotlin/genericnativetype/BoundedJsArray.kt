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
package genericnativetype

import jsinterop.annotations.JsOverlay
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

@JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
class BoundedJsArray<V> private constructor(size: Int) {
  companion object {
    /** Creates a new native JS array. */
    @JsOverlay
    @JvmStatic
    fun <V> create(): BoundedJsArray<V> {
      val array = null
      return array!!
    }

    /** Creates a new native JS array. */
    @JsOverlay
    @JvmStatic
    fun <V> create(size: Int): BoundedJsArray<V> {
      val array = BoundedJsArray<V>(size)
      return array
    }
  }
}
