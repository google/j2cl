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
package jsinnerclass

import jsinterop.annotations.JsType

class Outer {
  val a = 2

  @JsType(namespace = "com.google.test", name = "Inner")
  inner class Inner {
    private var b: Int

    constructor() {
      this.b = a + 1
    }

    fun getB(): Int = b
  }
}
