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
package innerclassinheritance

import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  val a = innerclassinheritance.p1.A()
  val aa = innerclassinheritance.p2.A() as innerclassinheritance.p1.A
  assertTrue(a.B() is innerclassinheritance.p1.A.B)
  assertTrue(aa.B() is innerclassinheritance.p1.A.B)
  assertTrue(aa.B() !is innerclassinheritance.p2.A.B)
  assertTrue(innerclassinheritance.p2.A().B() is innerclassinheritance.p2.A.B)
  assertTrue(innerclassinheritance.p2.A().C() is innerclassinheritance.p1.A.C)
}
