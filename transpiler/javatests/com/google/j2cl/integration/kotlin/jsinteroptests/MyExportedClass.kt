/*
 * Copyright 2015 Google Inc.
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
package jsinteroptests

import jsinterop.annotations.JsType

/** A test class that is exported. */
@JsType
class MyExportedClass {

  companion object {
    @JvmField var EXPORTED_1 = 100

    @JvmStatic fun foo(): Int = 200

    @JvmStatic fun replacementFoo(): Int = 1000

    @JvmField var EXPORTED_2 = InnerClass(5)

    @JvmStatic fun bar(a: Int, b: Int): Int = EXPORTED_2.`fun`(a, b)

    @JvmStatic fun newInnerClass(field: Int): InnerClass = InnerClass(field)
  }

  /** static inner JsType class. */
  @JsType
  class InnerClass(@JvmField var field: Int) {
    fun `fun`(a: Int, b: Int): Int {
      // return (a + b + |a - b| + field)
      // prevent optimizations from inlining this function
      var c = a + b
      if (a > b) {
        c = c + a - b
      } else {
        c = c + b - a
      }
      return c + field
    }
  }
}
