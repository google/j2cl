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
package jsbridgemultipleaccidental;

import com.google.j2cl.integration.testing.Asserts.assertTrue;

fun main(vararg args: String) {
  val a: InterfaceOne = Test();
  val b: InterfaceTwo = Test();
  val c: InterfaceThree = Test();
  val d: C = Test();
  val e: Test = Test();

  assertTrue(a.`fun`(1) == 1)
  assertTrue(b.`fun`(1) == 1)
  assertTrue(c.`fun`(1) == 1)
  assertTrue(d.`fun`(1) == 1)
  assertTrue(e.`fun`(1) == 1)
}
