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
import com.google.j2cl.integration.testing.TestUtils.isJvm
import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsType

object TestCase1596 {
  internal interface BI1 {
    fun get(value: String): String {
      return "BI1 get String"
    }
  }

  @JsType
  internal open class B<B1> : BI1 {
    fun get(value: B1): String {
      return "B get B1"
    }
  }

  internal class C @JsConstructor constructor() : B<String>()

  fun test() {
    val c = C()
    if (isJvm()) {
      // TODO(b/246407328): Override semantics and bridge construction differs from Java and J2CL
      assertTrue((c as B<*>).get("") == "BI1 get String")
      assertTrue((c as BI1).get("") == "BI1 get String")
    } else {
      assertTrue((c as B<*>).get("") == "B get B1")
      assertTrue((c as BI1).get("") == "B get B1")
    }
    // ambiguous method call
    // assertTrue(c.get("") == "B get B1")
  }
}
