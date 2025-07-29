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
package jsbridgemethodaccidentaloverride

import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg args: String) {
  val a: MyJsType = MyJsType()
  val b: SubJsType = SubJsType()
  val c: MyInterface = SubJsType()
  val d: OtherInterface = SubJsType()
  assertTrue(a.foo(1) == 1)
  assertTrue(b.foo(1) == 1)
  assertTrue(c.foo(1) == 1)
  assertTrue(a.bar(1) == 2)
  assertTrue(b.bar(1) == 3)
  assertTrue(c.bar(1) == 3)
  assertTrue(a.`fun`(1) == 0)
  assertTrue(b.`fun`(1) == 0)
  assertTrue(d.`fun`(1) == 0)
}
