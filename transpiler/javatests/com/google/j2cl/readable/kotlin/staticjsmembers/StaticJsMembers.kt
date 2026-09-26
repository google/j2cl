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
package staticjsmembers

import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsPackage.GLOBAL
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType
import kotlin.js.definedExternally

class StaticJsMembers {
  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  class Native {
    companion object {
      @JvmField
      @JsProperty(namespace = GLOBAL, name = "window")
      var window: Window = definedExternally

      @JvmField
      @JsProperty(namespace = GLOBAL, name = "window.window")
      var windowWindow: Window = definedExternally

      @JvmField
      @JsProperty(namespace = "foo.Bar", name = "field")
      var barField: Int = definedExternally
    }
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "window.Object") class Extern {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Window") class Window {}

  companion object {
    @JvmField @JsProperty(name = "field") var field1: Int = 0

    @JvmField @JsProperty var field2: Int = 0

    @JvmStatic @JsMethod(name = "fun") fun f1(a: Int) {}

    @JvmStatic @JsMethod fun f2(a: Int) {}

    @JvmStatic
    @JsMethod(namespace = JsPackage.GLOBAL, name = "Math.floor")
    external fun f3(a: Double)

    @JvmStatic @JsMethod(namespace = GLOBAL, name = "isFinite") external fun f4(a: Double)

    @JvmStatic @JsMethod(namespace = "foo.Bar", name = "baz") external fun f5()

    @JvmStatic @JsMethod(namespace = "foo.Baz", name = "baz") external fun f6()

    @JvmStatic
    @JsMethod(namespace = JsPackage.GLOBAL, name = "Math.max")
    external fun max(a: Int, b: Int): Int

    @JvmStatic
    @JsMethod(namespace = JsPackage.GLOBAL, name = "Math.max")
    external fun max(a: Int, b: Int, c: Int): Int

    @JvmStatic
    @JsMethod(namespace = JsPackage.GLOBAL, name = "Math.max")
    external fun max(a: Double, b: Double): Double
  }

  fun test() {
    StaticJsMembers.f1(1)
    f1(1)
    StaticJsMembers.f2(1)
    f2(1)
    StaticJsMembers.f3(1.1)
    f3(1.1)
    StaticJsMembers.f4(1.1)
    f4(1.1)
    StaticJsMembers.f5()
    f5()
    StaticJsMembers.max(1, 2)
    max(1, 2)
    StaticJsMembers.max(1, 2, 3)
    max(1, 2, 3)
    StaticJsMembers.max(1.0, 2.0)
    max(1.0, 2.0)

    var n: Int = field1
    n = field2

    var w: Window = Native.window
    w = Native.windowWindow
    n = Native.barField

    Native()
    Extern()
  }
}
