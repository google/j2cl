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

object Tester497 {
  @JsType
  internal open class C1<T> internal constructor() {
    fun get(value: T): String {
      return "C1.get"
    }
  }

  internal class C2 : C1<Any?>()

  fun test() {
    val s = C2()
    assertTrue((s as C1<Any>).get("") == "C1.get")
  }
}
