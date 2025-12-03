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

class Subclass : DefaultParams, DefaultParamInterface<String> {
  constructor() : super(b = 2)

  constructor(a: Int, b: Int) : super(a, b)

  override fun foo(c: Int) {
    super.foo(c + 10)
  }

  override fun parentInterfaceFun(a: Int) {}

  override fun <V> withTypeParameters(a: String?, b: V?): String? = a
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

fun testLocalFun() {
  fun localFun(a: Int, b: Int = 1) = a + b
  localFun(10)
  localFun(10, 20)

  fun localFunWithNonPrimitive(o: Any? = Any(), unused: Any? = null) = o

  val someValue = Any()
  localFunWithNonPrimitive()
  localFunWithNonPrimitive(someValue)
  localFunWithNonPrimitive("test")

  val nestedObj =
    object {
      fun doSomething() {
        fun nestedLocalFun(a: Int, b: Int = 1) = a + b

        nestedLocalFun(10)
        nestedLocalFun(10, 20)

        // Also try calling the local funs from the outer scope
        localFun(10)
        localFun(10, 20)

        localFunWithNonPrimitive()
        localFunWithNonPrimitive(someValue)
        localFunWithNonPrimitive("test")
      }
    }
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

fun captureDefault(str: String = "defaulted", f: () -> String = { "f: $str" }): String = f()

fun testCapturedDefault() {
  captureDefault()
  captureDefault("foo")
  captureDefault("foo") { "bar" }
}

fun identityOrCreate(o: Any? = Any(), unused: Any = Any()) = o

fun nestedDefaultCall(
  str: String?,
  nonNullStr: String,
  a: () -> Any? = { identityOrCreate() },
  b: () -> Any? = {
    // str is non-primitive so this will need to be coerced from undefined to null. Note that we
    // don't pass the unused parameter to ensure that we'll calling through the default bridge.
    identityOrCreate(str)
  },
  c: () -> Any? = {
    // Even though the string is non-null, we should still coerce undefined to null in case a null
    // has leaked. This should cause an NPE to be thrown rather than the default initializer to be
    // applied.
    identityOrCreate(nonNullStr)
  },
) {}

fun testDefaultInitializerCallsWithDefaults() {
  nestedDefaultCall("foo", "bar")
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

open class GenericSubclass<Z> : DefaultParams(1), DefaultParamInterface<Z> {
  override fun parentInterfaceFun(a: Int) = foo()

  override fun <V> withTypeParameters(a: Z?, b: V?): Z? = null
}

class ChildClass : GenericSubclass<String>()

fun <T : GenericSubclass<String>> T.callDefault() = withTypeParameters<Int>()

class OuterClass<T> {
  inner class InnerClass<U> {
    fun foo(a: U? = null): T? = null
  }
}

fun testGenerics() {
  var result = GenericSubclass<String>().withTypeParameters<Int>()
  result = ChildClass().withTypeParameters<Int>()

  result = GenericSubclass<String>().callDefault()
  result = ChildClass().callDefault()

  val inner = OuterClass<IFoo>().InnerClass<String>()
  val s = inner.foo()

  val i = (ChildClass() as DefaultParamInterface<*>).withTypeParameters<Int>()
  val j = (ChildClass() as DefaultParamInterface<*>).withTypeParameters<Int>(null)
}
