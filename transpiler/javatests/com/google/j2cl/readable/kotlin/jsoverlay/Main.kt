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
package jsoverlay

import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsOverlay
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

@JsType(isNative = true, namespace = "test.foo")
interface NativeJsTypeInterfaceWithOverlay {
  fun m(): Int

  @JsOverlay
  fun callM(): Int {
    return m()
  }

  companion object {
    @JvmField @JsOverlay val staticField = Any()
  }
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
class NativeJsTypeWithOverlayConstant {
  companion object {
    @JsOverlay const val COMPILE_TIME_CONSTANT = 10
    @JsOverlay const val STRING_COMPILE_TIME_CONSTANT = "10"
  }
}

@JsType(isNative = true, namespace = "test.foo")
class NativeJsTypeWithOverlay {
  external fun m(): Int

  @JsOverlay
  fun callM(): Int {
    return m()
  }

  @JsOverlay
  internal fun foo(): Int {
    return 1
  }

  @JsOverlay
  internal fun baz(): Int {
    return 1
  }

  @JsOverlay
  fun overlayWithJsFunction() {
    object : Intf {
        override fun run() {}
      }
      .run()
  }

  @JsOverlay fun overlay() {}

  companion object {
    @JvmField var nonJsOverlayField = 0

    @JvmField @JsOverlay var staticField: Any? = Any()

    // non JsOverlay static method.
    @JvmStatic external fun n()

    @JvmStatic
    @JsOverlay
    fun `fun`(): Int {
      return 1
    }

    @JvmStatic
    @JsOverlay
    internal fun bar(): Int {
      return 1
    }

    @JvmStatic
    @JsOverlay
    fun varargs(vararg a: Int): Int {
      return a[0]
    }

    @JvmStatic @JsOverlay fun overlay(o: NativeJsTypeWithOverlay?) {}
  }
}

@JsFunction
private fun interface Intf {
  fun run()
}

fun test() {
  val n = NativeJsTypeWithOverlay()
  n.callM()
  NativeJsTypeWithOverlay.`fun`()
  NativeJsTypeWithOverlay.n()
  NativeJsTypeWithOverlay.bar()
  n.foo()
  val a =
    NativeJsTypeWithOverlayConstant.COMPILE_TIME_CONSTANT +
      NativeJsTypeWithOverlay.nonJsOverlayField
  NativeJsTypeWithOverlay.staticField = null
  NativeJsTypeWithOverlay.varargs(1, 2, 3)
  n.baz()
  val b =
    (NativeJsTypeWithOverlayConstant.STRING_COMPILE_TIME_CONSTANT +
      NativeJsTypeInterfaceWithOverlay.staticField)
}

fun testOverlayInterface(foo: NativeJsTypeInterfaceWithOverlay) {
  foo.m()
  foo.callM()
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
internal interface ParameterizedNativeInterface<T> {
  @JsOverlay fun <T, S> shadowsTypeVariable(param1: T, param2: S) {}

  @JsOverlay fun <T, S> shadowsTypeVariable(param1: T, param2: Int) {}
}
