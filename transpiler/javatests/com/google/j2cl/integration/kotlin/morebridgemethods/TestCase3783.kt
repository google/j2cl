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

object TestCase3783 {
  internal open class B {
    fun get(value: String): String {
      return "B get String"
    }
  }

  @JsType
  internal interface CI1 {
    fun get(value: String): String
  }

  internal class C : B(), CI1

  fun test() {
    val c = C()
    assertTrue((c as B).get("") == "B get String")
    assertTrue(c.get("") == "B get String")
    assertTrue((c as CI1).get("") == "B get String")
  }
}
