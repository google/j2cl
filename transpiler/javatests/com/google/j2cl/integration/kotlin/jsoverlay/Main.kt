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

import com.google.j2cl.integration.testing.Asserts
import com.google.j2cl.integration.testing.AssertsBase
import jsinterop.annotations.JsOverlay
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

@JsType(isNative = true, namespace = "test.foo")
internal class NativeJsTypeWithOverlay {
  external fun m(): Int

  @JsOverlay
  fun callM(): Int {
    return m()
  }

  @JsOverlay
  internal fun foo(): Int {
    return 20
  }

  @JsOverlay
  internal fun baz(): Int {
    return 30
  }

  @JsOverlay
  internal fun getStaticField(): Any? {
    return Companion.staticField
  }

  companion object {
    @JvmField @JsOverlay var staticField: Any? = Any()

    @JvmStatic
    @JsOverlay
    fun func(a: Int, b: Int): Int {
      return if (a > b) a + b else a * b
    }

    @JvmStatic
    @JsOverlay
    internal fun bar(): Int {
      return 10
    }
  }
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
class NativeJsTypeWithOverlayConstant {
  companion object {
    @JsOverlay const val COMPILE_TIME_CONSTANT = 1
  }
}

@JsType(isNative = true, namespace = "test.foo")
internal class NativeFinalJsTypeWithOverlay {
  external fun e(): Int

  @JsOverlay
  fun buzz(): Int {
    return 42
  }
}

@JsType(isNative = true, namespace = "test.foo", name = "NativeJsTypeWithOverlay")
internal class NativesTypeWithOnlyPrivateOverlay<T> {
  companion object {
    @JsOverlay @JvmField internal val field = 42

    @JsOverlay
    @JvmStatic
    internal fun staticMethod(): Int {
      return field
    }
  }
}

fun testNativeJsWithOverlay() {
  val obj = NativeJsTypeWithOverlay()
  AssertsBase.assertTrue(6 == obj.callM())
  AssertsBase.assertTrue(20 == NativeJsTypeWithOverlay.func(4, 5))
  AssertsBase.assertTrue(1 == NativeJsTypeWithOverlayConstant.COMPILE_TIME_CONSTANT)
  AssertsBase.assertTrue(obj.getStaticField() != null)
  AssertsBase.assertTrue(NativeJsTypeWithOverlay.staticField != null)
  NativeJsTypeWithOverlay.staticField = null
  AssertsBase.assertTrue(NativeJsTypeWithOverlay.staticField == null)
  AssertsBase.assertTrue(10 == NativeJsTypeWithOverlay.bar())
  AssertsBase.assertTrue(20 == obj.foo())
  AssertsBase.assertTrue(30 == obj.baz())
  val f = NativeFinalJsTypeWithOverlay()
  AssertsBase.assertTrue(36 == f.e())
  AssertsBase.assertTrue(42 == f.buzz())
  Asserts.assertEquals(42, NativesTypeWithOnlyPrivateOverlay.staticMethod())
}

fun main(vararg unused: String) {
  testNativeJsWithOverlay()
}
