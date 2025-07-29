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
package bridgejsmethod

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsType

fun main(vararg args: String) {
  testJsMethods()
  testJsMethodForwarding()
  testJsMethodOverriddenInJs()
}

private open class A<T> {
  @JsMethod
  open fun `fun`(t: T): T {
    return t
  }

  // not a JsMethod.
  open fun bar(t: T): String = "A-bar"
}

/** Interface with JsMethod. */
interface I<T : Number?> {
  @JsMethod(name = "fNumber") fun `fun`(t: T): T
}

/** Interface with non-JsMethod. */
interface J<T> {
  fun bar(t: T): String
}

private open class B : A<String>() {
  // It is a JsMethod itself and also overrides a JsMethod.
  @JsMethod override fun `fun`(s: String): String = s + "abc"

  // It is a JsMethod itself and overrides a non-JsMethod.
  // Two bridges, one for the generic method, and one for exposing non-JsMethod, should result
  // in only one bridge.
  @JsMethod override fun bar(t: String): String = "B-bar"
}

private class C : A<Int?>() {
  // Not directly annotated as a JsMethod, but overrides a JsMethod.
  override fun `fun`(i: Int?): Int? = i!! + 5
}

private class D : A<Int?>(), I<Int?> {
  // Two JS bridges: fun__Object, and fun__Number, should result in one bridge.
  override fun `fun`(i: Int?): Int? = i!! + 6
}

private open class E : B(), J<String> {
  // inherited B.bar() accidentally overrides J.bar().
  // Two bridges, one for generic method, the other for exposing non-JsMethod, should result in
  // only one bridge.
}

private class F : E() {
  override fun bar(s: String): String = "F-bar"
}

private fun callFunByA(a: A<Any?>, o: Any?): Any? = a.`fun`(o)

private fun callBarByA(a: A<Any?>, t: Any?): String = a.bar(t)

private fun callFunByI(i: I<Number?>, o: Number?): Number? = i.`fun`(o)

private fun callBarByJ(j: J<Any?>, t: Any?): String = j.bar(t)

@JsMethod private external fun callFun(o: Any?, arg: Any?): Any?

@JsMethod private external fun callBar(o: Any?, t: Any?, s: Any?): Boolean

@Suppress("UNCHECKED_CAST")
private fun testJsMethods() {
  assertTrue(callFunByA(A<String>() as A<Any?>, "abc") == "abc")
  assertTrue(callFunByA(B() as A<Any?>, "def") == "defabc")
  assertTrue(callFunByA(C() as A<Any?>, 1) == 6)
  assertTrue(callFunByA(D() as A<Any?>, 2) == 8)
  assertTrue(callFunByA(E() as A<Any?>, "xyz") == "xyzabc")
  assertThrowsClassCastException({ callFunByA(B() as A<Any?>, 1) }, String::class.java)

  assertEquals("A-bar", callBarByA(A<String>() as A<Any?>, "abc"))
  assertEquals("B-bar", callBarByA(B() as A<Any?>, "abc"))
  assertEquals("A-bar", callBarByA(C() as A<Any?>, 1))
  assertEquals("A-bar", callBarByA(D() as A<Any?>, 2))
  assertEquals("B-bar", callBarByA(E() as A<Any?>, "xyz"))
  assertEquals("F-bar", callBarByA(F() as A<Any?>, "abcd"))
  assertThrowsClassCastException({ callBarByA(B() as A<Any?>, 1) }, String::class.java)

  assertTrue(callFunByI(D() as I<Number?>, 2) == 8)
  assertThrowsClassCastException({ callFunByI(D() as I<Number?>, 2.2f) }, Int::class.javaObjectType)

  assertEquals("B-bar", callBarByJ(E() as J<Any?>, "xyz"))
  assertThrowsClassCastException({ callBarByJ(E() as J<Any?>, Any()) }, String::class.java)

  assertTrue(B().`fun`("def") == "defabc")
  assertTrue(C().`fun`(1) == 6)
  assertTrue(D().`fun`(10) == 16)
  assertTrue(E().`fun`("xyz") == "xyzabc")

  assertEquals("A-bar", A<Any?>().bar(Any()))
  assertEquals("B-bar", B().bar("abcd"))
  assertEquals("A-bar", C().bar(1))
  assertEquals("A-bar", D().bar(1))
  assertEquals("B-bar", E().bar("abcd"))
  assertEquals("F-bar", F().bar("abcd"))

  assertTrue(callFun(A<String>(), "abc") == "abc")
  assertTrue(callFun(B(), "abc") == "abcabc")
  assertTrue(callFun(C(), 1) == 6)
  assertTrue(callFun(D(), 10) == 16)
  assertTrue(callFun(E(), "xyz") == "xyzabc")

  assertTrue(callBar(B(), "abcd", "defg"))
  assertTrue(callBar(E(), "abcd", "defg"))
}

private open class Top<T, S> {
  @JsMethod open fun m(t: T, s: S) {}
}

interface Accidental<S> {
  fun m(t: String?, s: S)
}

private open class Child<S> : Top<String?, S>() {
  override fun m(t: String?, s: S) {}
}

private class GrandChild : Child<String?>(), Accidental<String?> {
  override fun m(t: String?, s: String?) {}
}

@Suppress("UNCHECKED_CAST")
private fun testJsMethodForwarding() {
  val o: Top<Any?, Any?> = GrandChild() as Top<Any?, Any?>
  assertThrowsClassCastException({ o.m(null, Any()) }, String::class.java)

  assertThrowsClassCastException({ o.m(Any(), null) }, String::class.java)

  val a: Accidental<Any?> = GrandChild() as Accidental<Any?>
  assertThrowsClassCastException({ a.m(null, Any()) }, String::class.java)
}

interface InterfaceWithMethod {
  fun m(): String
}

interface InterfaceWithDefaultJsMethod : InterfaceWithMethod {
  @JsMethod override fun m(): String = "m from Java default"

  @JsMethod fun n(): String
}

open class SuperClassWithFinalMethod {
  fun n(): String = "n from Java"
}

open class ExposesOverrideableJsMethod : SuperClassWithFinalMethod(), InterfaceWithDefaultJsMethod

private fun testJsMethodOverriddenInJs() {
  assertEquals("m from Java default", ExposesOverrideableJsMethod().m())
  assertEquals("m from Java default", (ExposesOverrideableJsMethod() as InterfaceWithMethod).m())
  assertEquals("m from Js", NativeSubclass().m())
  assertEquals("m from Js", (NativeSubclass() as InterfaceWithMethod).m())

  assertEquals("n from Java", ExposesOverrideableJsMethod().n())
  assertEquals("n from Java", (ExposesOverrideableJsMethod() as SuperClassWithFinalMethod).n())
  assertEquals("n from Js", NativeSubclass().n())
  // TODO(b/280451858): uncomment when bug is fixed.
  // assertEquals("n from Js", ((NativeSubclass() as Any) as SuperClassWithFinalMethod).n());
}

@JsType(isNative = true)
class NativeSubclass {
  external fun m(): String

  external fun n(): String
}
