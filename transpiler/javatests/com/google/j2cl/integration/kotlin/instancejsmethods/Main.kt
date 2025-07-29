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
package instancejsmethods

import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod

fun main(vararg args: String) {
  testCallByConcreteType()
  testCallBySuperParent()
  testCallByJS()
}

fun testCallBySuperParent() {
  val sp: SuperParent = SuperParent()
  val p: SuperParent = Parent()
  val c: SuperParent = Child()
  val pp: Parent = Parent()
  val cc: Parent = Child()
  val intf: MyInterface = Child()

  assertTrue(sp.`fun`(12, 35) == 158)
  assertTrue(sp.bar(6, 7) == 264)
  assertTrue(p.`fun`(12, 35) == 47)
  assertTrue(p.bar(6, 7) == 42)
  assertTrue(c.`fun`(12, 35) == 48)
  assertTrue(c.bar(6, 7) == 43)
  assertTrue(pp.foo(10) == 10)
  assertTrue(cc.foo(10) == 11)
  assertTrue(intf.intfFoo(5) == 5)
}

fun testCallByConcreteType() {
  val sp: SuperParent = SuperParent()
  val p: Parent = Parent()
  val c: Child = Child()

  assertTrue(sp.`fun`(12, 35) == 158)
  assertTrue(sp.bar(6, 7) == 264)
  assertTrue(p.`fun`(12, 35) == 47)
  assertTrue(p.bar(6, 7) == 42)
  assertTrue(c.`fun`(12, 35) == 48)
  assertTrue(c.bar(6, 7) == 43)
  assertTrue(p.foo(10) == 10)
  assertTrue(c.foo(10) == 11)
  assertTrue(c.intfFoo(5) == 5)
}

fun testCallByJS() {
  val p: Parent = Parent()
  val c: Child = Child()
  assertTrue(callParentFun(p, 12, 35) == 47)
  assertTrue(callParentBar(p, 6, 7) == 42)
  assertTrue(callParentFoo(p, 10) == 10)
  assertTrue(callChildFun(c, 12, 35) == 48)
  assertTrue(callChildBar(c, 6, 7) == 43)
  assertTrue(callChildFoo(c, 10) == 11)
  assertTrue(callChildIntfFoo(c, 5) == 5)
}

@JsMethod external fun callParentFun(p: Parent, a: Int, b: Int): Int

@JsMethod external fun callParentBar(p: Parent, a: Int, b: Int): Int

@JsMethod external fun callParentFoo(p: Parent, a: Int): Int

@JsMethod external fun callChildFun(c: Child, a: Int, b: Int): Int

@JsMethod external fun callChildBar(c: Child, a: Int, b: Int): Int

@JsMethod external fun callChildFoo(c: Child, a: Int): Int

@JsMethod external fun callChildIntfFoo(c: Child, a: Int): Int
