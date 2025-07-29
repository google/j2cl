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

import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.TestUtils.isJvm
import java.util.function.Consumer

object TestCaseHand2 {

  internal interface I<I1> {
    fun get(consumer: Consumer<in I1>?): String
  }

  internal abstract class B<B1, B2> : I<B1> {
    fun get(consumer: B2): String {
      return "B get B2"
    }
  }

  internal class C<C1> : B<C1, Consumer<in C1>>(), I<C1> {
    override fun get(consumer: Consumer<in C1>?): String {
      return "C get String"
    }
  }

  fun test() {
    val c = C<Any>()
    if (isJvm()) {
      // TODO(b/246407328): Override semantics and bridge construction differs from Java and J2CL
      assertTrue((c as B<Any?, Any?>).get(null as String?) == "B get B2")
      assertTrue(c.get(null as String?) == "B get B2")
    } else {
      assertThrowsClassCastException({ (c as B<Any, Any>).get("") }, "java.util.function.Consumer")
      assertTrue((c as B<Any?, Any?>).get(null as String?) == "C get String")
      assertTrue(c.get(null as String?) == "C get String")
    }
    assertTrue(c.get(null) == "C get String")
  }
}
