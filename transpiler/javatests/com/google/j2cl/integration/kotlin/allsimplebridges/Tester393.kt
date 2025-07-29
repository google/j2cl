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

object Tester393 {
  @JsType
  internal abstract class C1 internal constructor() {
    abstract fun get(value: Any): String
  }

  internal class C2 : C1() {
    override fun get(value: Any): String {
      return "C2.get"
    }

    fun get(value: String): String {
      return "C2.get"
    }
  }

  fun test() {
    val s = C2()
    assertTrue(s.get(Any()) == "C2.get")
    assertTrue(s.get("") == "C2.get")
    assertTrue((s as C1).get("") == "C2.get")
  }
}
