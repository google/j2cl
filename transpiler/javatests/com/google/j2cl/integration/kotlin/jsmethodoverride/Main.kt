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
package jsmethodoverride

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

fun main(vararg args: String) {
  testJsMethodOverride()
  testJsMethodOverrideWithBridges()
}

open class A {
  @JsMethod(name = "y") open fun x(): Int = 1
}

@JsType
class B : A() {
  override fun x(): Int = 2
}

@JsMethod external fun callY(o: Any?): Int

fun testJsMethodOverride() {
  val a = A()
  val b = B()
  assertTrue(1 == callY(a))
  assertTrue(2 == callY(b))
}

@JsType
open class Parent<T> {
  open fun withoutOverrideRenamed(t: T): String = "Parent.notRenamed"

  open fun withOverrideRenamed(t: T): String = "Parent.toBeRenamed"
}

@JsType
class Child : Parent<String>() {
  override fun withoutOverrideRenamed(s: String): String = "Child.notRenamed"

  @JsMethod(name = "withNewName")
  override fun withOverrideRenamed(s: String): String = "Child.renamed"
}

@JsType(isNative = true, name = "?", namespace = JsPackage.GLOBAL)
interface AllMethods {
  fun withoutOverrideRenamed(o: Any?): String?

  fun withOverrideRenamed(o: Any?): String?

  fun withNewName(o: Any?): String?
}

@Suppress("UNCHECKED_CAST")
fun testJsMethodOverrideWithBridges() {
  var p: Parent<*> = Parent<Any>()
  assertEquals("Parent.notRenamed", (p as Parent<Any>).withoutOverrideRenamed(Any()))
  assertEquals("Parent.toBeRenamed", (p as Parent<Any>).withOverrideRenamed(Any()))
  p = Child()
  assertEquals("Child.notRenamed", (p as Parent<String>).withoutOverrideRenamed(""))
  assertEquals("Child.renamed", (p as Parent<String>).withOverrideRenamed(""))

  val c = Child()
  assertEquals("Child.notRenamed", c.withoutOverrideRenamed(""))
  assertEquals("Child.renamed", c.withOverrideRenamed(""))

  val am = c as AllMethods
  assertEquals("Child.notRenamed", am.withoutOverrideRenamed(""))
  // Call through the bridged method
  assertEquals("Child.renamed", am.withOverrideRenamed(""))
  // Call directly through the new name
  assertEquals("Child.renamed", am.withNewName(""))
}
