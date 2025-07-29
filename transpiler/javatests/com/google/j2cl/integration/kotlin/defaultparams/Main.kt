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
package defaultparams

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertNull
import com.google.j2cl.integration.testing.TestUtils.getUndefined
import jsinterop.annotations.JsConstructor

/** Test for default params. */
fun main(vararg unused: String) {
  testConstructors()
  testJsConstructors()
  testMethods()
  testExtFun()
  testComplexDefault()
  testVarargs()
  testInterface()
  testUninitialized()
}

open class DefaultParams(val a: Int = 1, val b: Int) {
  constructor(a: Int) : this(b = a + 1) {}

  open fun foo(c: Int = 20): Int {
    return a + b + c
  }
}

class Subclass : DefaultParams {
  constructor() : super(b = 2)

  constructor(a: Int, b: Int) : super(a, b)

  override fun foo(c: Int): Int {
    return super.foo(c + 10)
  }
}

open class JsConstructorType @JsConstructor constructor(val x: Int)

class JsConstructorSubtype @JsConstructor constructor(a: Int = 1, b: Int = 2, c: Int = 3) :
  JsConstructorType(a + b + c)

private fun testConstructors() {
  assertEquals(123, DefaultParams(a = 123, b = 456).a)
  assertEquals(456, DefaultParams(a = 123, b = 456).b)

  assertEquals(1, DefaultParams(b = 123).a)
  assertEquals(123, DefaultParams(b = 123).b)

  assertEquals(1, DefaultParams(a = 123).a)
  assertEquals(124, DefaultParams(a = 123).b)

  assertEquals(1, Subclass().a)
  assertEquals(2, Subclass().b)

  assertEquals(123, Subclass(123, 456).a)
  assertEquals(456, Subclass(123, 456).b)
}

private fun testJsConstructors() {
  assertEquals(6, JsConstructorSubtype().x)
  assertEquals(15, JsConstructorSubtype(a = 10).x)
  assertEquals(14, JsConstructorSubtype(b = 10).x)
  assertEquals(13, JsConstructorSubtype(c = 10).x)
  assertEquals(60, JsConstructorSubtype(a = 10, b = 20, c = 30).x)
}

private fun testMethods() {
  assertEquals(23, DefaultParams(b = 2).foo())
  assertEquals(6, DefaultParams(b = 2).foo(3))
  assertEquals(18, Subclass().foo(5))
}

fun DefaultParams.extFun() = foo()

fun DefaultParams.extFunWithDefault(c: Int = 3) = foo(c)

private fun testExtFun() {
  assertEquals(23, DefaultParams(b = 2).extFun())
  assertEquals(6, DefaultParams(b = 2).extFunWithDefault())
  assertEquals(7, DefaultParams(b = 2).extFunWithDefault(4))
}

fun complexDefault(
  a: Int,
  b: Int =
    when {
      a < 0 -> a * -1
      else -> a + 1
    },
): Int {
  return b
}

private fun testComplexDefault() {
  assertEquals(1, complexDefault(a = -1))
  assertEquals(2, complexDefault(a = 1))
  assertEquals(123, complexDefault(a = 1, b = 123))
}

fun boxing(a: Int? = 1) = a

fun capturedBoxing(a: Int? = null): Int {
  if (a is Int) {
    class LocalClass(val b: Int = a + 1)
    return LocalClass().b
  }
  return -1
}

fun testBoxing() {
  assertEquals(1, boxing())
  assertEquals(2, boxing(2))
  assertNull(boxing(null))

  assertEquals(-1, capturedBoxing())
  assertEquals(2, capturedBoxing(1))
}

fun optionalVarargs(vararg args: Int = intArrayOf(1, 2, 3)) = args

fun varargsWithTrailingOptional(vararg args: Int, optional: Int = 10) = optional

fun varargsWithLeadingOptional(optional: Int = 10, vararg args: Int) = optional

fun testVarargs() {
  var actual = optionalVarargs()
  assertEquals(1, actual[0])
  assertEquals(2, actual[1])
  assertEquals(3, actual[2])
  actual = optionalVarargs(4, 5, 6)
  assertEquals(4, actual[0])
  assertEquals(5, actual[1])
  assertEquals(6, actual[2])

  assertEquals(10, varargsWithTrailingOptional(4, 5, 6))
  assertEquals(20, varargsWithTrailingOptional(4, 5, 6, optional = 20))
  assertEquals(20, varargsWithTrailingOptional(optional = 20))

  assertEquals(10, varargsWithLeadingOptional(args = intArrayOf(4, 5, 6)))
  assertEquals(20, varargsWithLeadingOptional(20, 4, 5, 6))
  assertEquals(20, varargsWithLeadingOptional(20))
}

interface IFoo {
  fun defaultMethod(a: Int = 1) = a

  fun interfaceMethod(a: Int = 2): Int
}

class FooImpl : IFoo {
  override fun interfaceMethod(a: Int) = a
}

class FooImplWithDefaultOverride : IFoo {
  override fun defaultMethod(a: Int) = a + 1

  override fun interfaceMethod(a: Int) = a
}

private fun testInterface() {
  assertEquals(1, FooImpl().defaultMethod())
  assertEquals(2, FooImpl().defaultMethod(2))
  assertEquals(2, FooImpl().interfaceMethod())
  assertEquals(3, FooImpl().interfaceMethod(3))
  assertEquals(3, FooImplWithDefaultOverride().defaultMethod(2))
}

fun strOrDefault(str: String? = "defaulted", unused: Any? = null) = str

fun boxedIntOrDefault(i: Int? = -1, unused: Any? = null) = i

fun boxedDoubleOrDefault(d: Double? = -1.0, unused: Any? = null) = d

private fun testUninitialized() {
  // Unlike JS, we don't want to treating passing undefined as not passing the parameter. Therefore
  // all of these calls should treat passing undefined as-if we had passed null.
  assertNull(strOrDefault(getUndefined()))
  assertNull(boxedIntOrDefault(getUndefined()))
  assertNull(boxedDoubleOrDefault(getUndefined()))
}
