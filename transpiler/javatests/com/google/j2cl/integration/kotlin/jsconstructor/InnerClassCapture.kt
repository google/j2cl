/*
 * Copyright 2023 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package jsconstructor

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.TestUtils
import jsinterop.annotations.JsConstructor

object InnerClassCapture {

  abstract class Base() {
    init {
      callFromCtor()
    }

    abstract fun callFromCtor()
  }

  abstract class BaseJsConstructor @JsConstructor constructor() {
    init {
      callFromCtor()
    }

    abstract fun callFromCtor()
  }

  class Outer {
    var i = 1

    inner class Inner @JsConstructor constructor() : Base() {
      fun getEnclosingValue() = i

      override fun callFromCtor() {
        // Referencing Outer.i here requires that outer class capture is resolved before we invoke
        // the BaseType ctor as this function will be invoked by that ctor.
        i += 2
      }
    }

    inner class InnerExtendsJsConstructor @JsConstructor constructor() : BaseJsConstructor() {
      fun getEnclosingValue() = i

      override fun callFromCtor() {
        // Referencing Outer.i here requires that outer class capture is resolved before we invoke
        // the BaseType ctor as this function will be invoked by that ctor.
        // However, if the parent type is JsConstructor, we cannot make this binding before
        // super() gets called.
        if (this@Outer == null) {
          failedInCtor = true
        } else {
          i += 3
        }
      }
    }
  }

  var failedInCtor = false

  fun test() {
    val outer = Outer()

    assertEquals(1, outer.i)

    val inner = outer.Inner()
    assertEquals(3, outer.i)
    assertEquals(3, inner.getEnclosingValue())

    // J2CL cannot properly emulate the JVM semantics of ensuring the outer class binding is
    // resolved before calling the super constructor. This is due to a limitation of ES6 classes
    // where the instance cannot be referenced before the super ctor is called. The binding will be
    // resolved just after the super constructor is called.
    failedInCtor = false
    val innerJsConstructor = outer.InnerExtendsJsConstructor()

    if (!TestUtils.isJavaScript()) {
      assertFalse(failedInCtor)
      assertEquals(6, innerJsConstructor.getEnclosingValue())
    } else {
      assertTrue(failedInCtor)
      assertEquals(3, innerJsConstructor.getEnclosingValue())
    }
  }
}
