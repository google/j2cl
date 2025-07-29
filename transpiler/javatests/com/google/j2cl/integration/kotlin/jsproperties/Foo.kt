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
package jsproperties

import jsinterop.annotations.JsProperty

/** Tests for non native static JsProperty. */
class Foo {
  companion object {
    @JvmStatic private var f = 10

    @JvmStatic fun getF() = f

    @JvmStatic @JsProperty fun getA() = f + 1

    @JvmStatic
    @JsProperty
    fun setA(a: Int) {
      f = a + 2
    }

    @JvmStatic @JsProperty(name = "abc") fun getB() = f + 3

    @JvmStatic
    @JsProperty(name = "abc")
    fun setB(a: Int) {
      f = a + 4
    }
  }
}
