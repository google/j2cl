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

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsType

fun main(vararg args: String) {
  testCallByConcreteType()
  testCallBySuperParent()
  testCallByJS()
  testOverloads()
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

@JsMethod(namespace = "instancejsmethods.helper")
external fun callParentFun(p: Parent, a: Int, b: Int): Int

@JsMethod(namespace = "instancejsmethods.helper")
external fun callParentBar(p: Parent, a: Int, b: Int): Int

@JsMethod(namespace = "instancejsmethods.helper") external fun callParentFoo(p: Parent, a: Int): Int

@JsMethod(namespace = "instancejsmethods.helper")
external fun callChildFun(c: Child, a: Int, b: Int): Int

@JsMethod(namespace = "instancejsmethods.helper")
external fun callChildBar(c: Child, a: Int, b: Int): Int

@JsMethod(namespace = "instancejsmethods.helper") external fun callChildFoo(c: Child, a: Int): Int

@JsMethod(namespace = "instancejsmethods.helper")
external fun callChildIntfFoo(c: Child, a: Int): Int

class Overloads @JsConstructor constructor() {
  @JsMethod(name = "mObject")
  fun m(o: Any?): String {
    return "m(Object)"
  }

  @JsMethod(name = "mDouble")
  fun m(d: Double?): String {
    return "m(Double)"
  }
}

@JsType(isNative = true, namespace = "instancejsmethods", name = "Overloads")
class NativeOverloads {
  external fun mObject(o: Any?): String

  external fun mDouble(d: Double?): String

  @JsMethod(name = "mObject") external fun m(o: Any?): String

  @JsMethod(name = "mDouble") external fun m(o: Double?): String
}

private fun testOverloads() {
  val overloads = Overloads()
  assertEquals("m(Object)", overloads.m(Any()))
  assertEquals("m(Double)", overloads.m(0.0))

  val nativeOverloads = NativeOverloads()
  // Check overloaded JsMethods.
  assertEquals("m(Object)", nativeOverloads.mObject(null))
  assertEquals("m(Double)", nativeOverloads.mDouble(0.0))

  // Check overloaded native methods.
  assertEquals("m(Object)", nativeOverloads.m(Any()))
  assertEquals("m(Double)", nativeOverloads.m(0.0))
}
