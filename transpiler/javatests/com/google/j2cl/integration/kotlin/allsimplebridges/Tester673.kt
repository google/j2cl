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
package allsimplebridges

import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsType

object Tester673 {
  @JsType
  internal interface I1 {
    fun get(value: String): String {
      return "I1.get"
    }
  }

  internal class C1 : I1 {
    fun get(value: Any): String {
      return "C1.get"
    }
  }

  fun test() {
    val s = C1()
    assertTrue(s.get(Any()) == "C1.get")
    assertTrue((s as I1).get("") == "I1.get")
  }
}
