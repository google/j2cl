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
package nativejstypes

import jsinterop.annotations.JsType
import kotlin.js.definedExternally

/** Native JsType without "namespace" or "name". */
@JsType(isNative = true)
class Bar(x: Int, y: Int) {
  @JvmField var x: Int = definedExternally
  @JvmField var y: Int = definedExternally

  external fun product(): Int

  companion object {
    @JvmField var f: Int = definedExternally

    @JvmStatic external fun getStatic()

    fun product(): Int = 2
  }

  @JsType(isNative = true)
  class Inner(n: Int) {
    external fun square(): Int

    companion object {
      @JvmStatic external fun getInnerStatic(): Int
    }
  }
}

/** Native inner JsType. */
@JsType(isNative = true, name = "Bar.Inner")
class BarInnerWithDotInName(n: Int) {
  external fun square(): Int

  companion object {
    @JvmStatic external fun getInnerStatic(): Int
  }
}

@JsType(isNative = true, namespace = "nativejstypes", name = "Bar.Inner")
class BarInnerWithDotInNameAndHasNamespace(n: Int) {
  external fun square(): Int

  companion object {
    @JvmStatic external fun getInnerStatic(): Int
  }
}
