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

import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsType

@JsType(isNative = true, namespace = "com.google.test")
class NativeType {
  companion object {
    // return Inner(outer).getB()
    @JvmStatic internal external fun getB(outer: Outer): Int
  }
}

fun main(vararg args: String) {
  // Call the constructor through js
  assertTrue(NativeType.getB(Outer()) == 3)
  // Call the constructor through java
  assertTrue(Outer().Inner().getB() == 3)
}
