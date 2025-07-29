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
package morebridgemethods

import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsType

object TestCase11830 {
  internal interface CI3 {
    fun get(value: String): String
  }

  @JsType
  internal interface CI1 : CI3 {
    override fun get(value: String): String {
      return "CI1 get String"
    }
  }

  internal class C<C1> : CI1

  fun test() {
    val c = C<Any>()
    assertTrue(c.get("") == "CI1 get String")
    assertTrue(c.get("") == "CI1 get String")
  }
}
