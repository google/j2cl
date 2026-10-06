/*
 * Copyright 2022 Google Inc.
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
package externs

import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType
import kotlin.js.definedExternally

class Main {
  @JsType(isNative = true, namespace = JsPackage.GLOBAL)
  class TopLevelExtern {
    @JsType(isNative = true) class InnerExtern

    @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Foo")
    class Inner(str: String?) {
      @JvmField var foo: String? = definedExternally

      external fun instanceMethod(): String?

      companion object {
        @JvmField var staticField: Int = definedExternally

        @JvmStatic external fun staticMethod(): Int
      }
    }
  }

  var innerExtern: TopLevelExtern.InnerExtern? = null
  var inner: TopLevelExtern.Inner? = null
}

fun main(args: Array<String>) {
  val inner = Main.TopLevelExtern.Inner("Hello")
  Main.TopLevelExtern.Inner.staticField = Main.TopLevelExtern.Inner.staticMethod()
  inner.foo = inner.instanceMethod()
}
