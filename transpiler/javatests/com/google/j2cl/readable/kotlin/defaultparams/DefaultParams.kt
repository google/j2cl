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

open class DefaultParams(val a: Int = 1, val b: Int) {
  constructor(a: Int) : this(b = a) {}

  open fun foo(c: Int = 20) {}
}

class Subclass : DefaultParams {
  constructor() : super(b = 2)

  constructor(a: Int, b: Int) : super(a, b)

  override fun foo(c: Int) {
    super.foo(c + 10)
  }
}

fun testConstructors() {
  DefaultParams(a = 123)
  DefaultParams(b = 123)
  DefaultParams(a = 234, b = 456)
  Subclass()
  Subclass(123, 456)
}

fun testMethods() {
  DefaultParams(b = 123).foo()
  DefaultParams(b = 123).foo(456)
  Subclass().foo(456)
}

fun DefaultParams.extFun() = foo()

fun DefaultParams.extFunWithDefault(c: Int = -1) = foo(c)

fun testExtFun() {
  DefaultParams(b = 2).extFun()
  DefaultParams(b = 2).extFunWithDefault()
  DefaultParams(b = 2).extFunWithDefault(123)
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

fun testComplexDefault() {
  complexDefault(a = -1)
  complexDefault(a = 1)
  complexDefault(a = 1, b = 123)
}

fun boxing(a: Int? = 1) {}

fun capturedBoxing(a: Int? = null): Int {
  if (a is Int) {
    class LocalClass(val b: Int = a)
    return LocalClass().b
  }
  return -1
}

fun testBoxing() {
  boxing()
  boxing(1)
  boxing(null)

  capturedBoxing()
  capturedBoxing(1)
}

fun optionalVarargs(vararg args: Int = intArrayOf(1, 2, 3)) {}

fun varargsWithTrailingOptional(vararg args: Int, optional: Int = 10) {}

fun varargsWithLeadingOptional(optional: Int = 10, vararg args: Int) {}

fun optionalVarargsWithLeadingOptional(
  optional: Int = 10,
  vararg args: Int = intArrayOf(1, 2, 3),
) {}

fun testVarargs() {
  optionalVarargs()
  optionalVarargs(4, 5, 6)
  varargsWithTrailingOptional(4, 5, 6)
  varargsWithTrailingOptional(4, 5, 6, optional = 20)
  varargsWithTrailingOptional(optional = 20)
  varargsWithLeadingOptional(args = intArrayOf(4, 5, 6))
  varargsWithLeadingOptional(20, 4, 5, 6)
  varargsWithLeadingOptional(20)
  optionalVarargsWithLeadingOptional()
  optionalVarargsWithLeadingOptional(args = intArrayOf(4, 5, 6))
  optionalVarargsWithLeadingOptional(args = intArrayOf(4, 5, 6), optional = 20)
  optionalVarargsWithLeadingOptional(20, 4, 5, 6)
  optionalVarargsWithLeadingOptional(20)
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

fun testInterface() {
  FooImpl().defaultMethod()
  FooImpl().defaultMethod(2)
  FooImpl().interfaceMethod()
  FooImpl().interfaceMethod(2)
  FooImplWithDefaultOverride().defaultMethod(2)
}
