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
// Suppress warning "expected performance impact from inlining is insignificant" when inline
// functions do not use lambda.
@file:Suppress("NOTHING_TO_INLINE")

package inlinefunction

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertThrows
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.Asserts.fail
import com.google.j2cl.integration.testing.TestUtils.isJvm

fun main(vararg unused: String) {
  testInlining()
  testReifiedParameter()
  testInliningWithLocalClass()
  testCrossModuleInlining()
  testNoInline()
  testPropertyInlining()
  testFunctionRef()
  testExtensionReceiver()
  testDefaultParameters()
  testReturnNothing()
  testNestedInlineFun()
  testStdlibInlineFun()
  testNonReifiedTypeParameterErasureInInlineFun()
  testInlinedLocalClassSemantics()
  testInlineFactoryFunctionWithCrossinlineLambda()
  testDefaultParams()
  testBreakContinue()
  testInlineWithVararg()
  testProjectedReceiverTypeArgument()
}

class MyClass(var f: Int) {
  fun double() {
    f *= 2
  }
}

inline fun topLevelInlineFunction(myClass: MyClass, action: (Int) -> Int): Int {
  return action(myClass.f)
}

inline fun <T, R> doSomethingOn(target: T, block: (T) -> R) = block(target)

inline fun MyClass.extensionInlineFunction(action: (Int) -> Int): Int {
  var sum = f
  while (f > 0) {
    sum += action(f--)
  }

  return sum
}

inline fun MyClass.extensionInlineFunctionNoReturn(action: (Int) -> Int) {
  f = action(f)
}

inline fun inlineFunCallingInlineFun(myClass: MyClass, action: (Int) -> Int): Int =
  topLevelInlineFunction(myClass) { action(it) * 2 }

interface DefaultMethodCallingInlineFunction {
  private inline fun <reified T> checkType(obj: Any, thenFun: () -> Unit, elseFun: () -> Unit) {
    if (obj is T) {
      thenFun()
    } else {
      elseFun()
    }
  }

  fun callInlineFunctions() {
    checkType<String>("String", thenFun = {}, elseFun = { fail() })
    checkType<String>(1, thenFun = { fail() }, elseFun = {})
  }
}

class TestDefaultMethodCallingInlineFunction : DefaultMethodCallingInlineFunction

private fun testInlining() {
  val a = ClassWithInlineFun(5).inlineFun() { it * 2 }
  assertEquals(15, a)

  val b = MyClass(3).extensionInlineFunction { it * 2 }
  assertTrue(b == 15)

  val c =
    MyClass(3).extensionInlineFunction { if (it < 2) return@extensionInlineFunction 0 else it * 2 }
  assertTrue(c == 13)

  val d = MyClass(5)
  d.extensionInlineFunctionNoReturn { it * 2 }
  assertTrue(d.f == 10)

  val e = topLevelInlineFunction(MyClass(2)) { it * 2 }
  assertTrue(e == 4)

  val f = inlineFunCallingInlineFun(MyClass(2)) { it * 2 }
  assertTrue(f == 8)

  var anInt = 0
  val g =
    topLevelInlineFunction(MyClass(3)) {
      anInt = it * 3
      anInt
    }
  assertTrue(g == 9)
  assertTrue(anInt == 9)

  TestDefaultMethodCallingInlineFunction().callInlineFunctions()

  topLevelInlineFunction(MyClass(2)) {
    return
  }

  fail("Unreachable code")
}

inline fun <reified T> castTo(obj: Any?): T {
  return obj as T
}

inline fun <reified T> instanceOf(obj: Any): Boolean = obj is T

// Overload Int so that we have a variant that takes a primitive.
inline fun <reified T> instanceOf(i: Int): Boolean = i is T

fun testReifiedParameter() {
  var aStringAsAny: Any = "foo"
  var nullAny: Any? = null

  val a = castTo<String>(aStringAsAny)
  assertTrue(a is String)

  assertThrowsClassCastException {
    val a = castTo<Error>(aStringAsAny)
  }

  val b = castTo<String?>(nullAny)
  assertTrue(b is String?)

  assertThrowsNullPointerException {
    val b = castTo<String>(nullAny)
  }

  assertTrue(instanceOf<Int>(1))
  assertTrue(instanceOf<String>(aStringAsAny))
  assertFalse(instanceOf<Double>(1))
  assertFalse(instanceOf<String>(1))
  assertFalse(instanceOf<Double>(aStringAsAny))

  var someInt = 1
  assertTrue(instanceOf<Int>(++someInt))
  assertFalse(instanceOf<Double>(++someInt))
  assertEquals(3, someInt)
}

private inline fun inlineFunWithAnonymousObject(captureParam: Int): Int {
  val capture = 5
  val delegate =
    object {
      fun compute(): Int {
        return (capture + captureParam) * 2
      }
    }
  return delegate.compute()
}

private fun testInliningWithLocalClass() {
  val a =
    topLevelInlineFunction(MyClass(2)) {
      class Foo(val f: Int) {
        fun mulByTwo() = f * 2
      }
      Foo(it).mulByTwo()
    }

  assertTrue(a == 4)

  val b = inlineFunWithAnonymousObject(5)
  assertTrue(b == 20)
}

private fun testCrossModuleInlining() {
  val a = inlineFunctionFromDeps(ExternalClass(2)) { it * 2 }
  assertEquals(4, a)

  val b = inlineFunctionFromDepsCallingJava { it * 2 }
  // (1 + 2 + 3) * 2
  assertEquals(12, b)

  // TODO(b/567869389): Uncomment when the outer `this` of an inner class and private top-level
  // properties can be accessed from an inline function inlined in another module.
  // val c = OuterClass(1).InnerClass(10).sumWithPrivateMembers()
  // assertEquals(1111, c)
}

inline fun inlineFunWithNoInlineParam(noinline notInlined: () -> Int): Any {
  return notInlined
}

fun testNoInline() {
  val intProducer = inlineFunWithNoInlineParam { 5 } as () -> Int
  assertTrue(intProducer.invoke() == 5)
}

var value: Int = 0

inline var inlineProp: Int
  get() = ++value
  set(p: Int) {
    value = p
  }

fun testPropertyInlining() {
  val a = inlineProp
  assertTrue(a == 1)
  assertTrue(value == 1)

  inlineProp = a + 2
  assertTrue(value == 3)

  inlineProp += 1
  assertTrue(value == 5)
}

inline fun runNTimes(n: Int, action: () -> Unit) {
  for (i in 0..<n) {
    action()
  }
}

fun testFunctionRef() {
  val foo = MyClass(2)
  doSomethingOn(foo, MyClass::double)
  assertEquals(4, foo.f)

  val result =
    doSomethingOn(MyClass(1)) {
      runNTimes(4, it::double)
      it.f
    }
  assertEquals(16, result)
}

inline fun (() -> String).test(): (() -> String) = { invoke() + this.invoke() + this() }

fun testExtensionReceiver() {
  val a = { "OK" }.test()()
  assertEquals("OKOKOK", a)
}

inline fun <reified T> inlineFunWithDefaultLambda(
  any: Any,
  lambda: (any: Any) -> Boolean = { it is T },
): Boolean {
  return lambda(any)
}

var this_is_true = true

inline fun inlineFunWithDefaultParams(
  s: String = "default_s",
  i: Int = if (this_is_true) 2 else 3,
  lambda: (s: String, i: Int) -> String = { s1: String, i1: Int ->
    "Lambda default " + s1 + " " + i1
  },
): String {
  return lambda(s, i)
}

fun testDefaultParameters() {
  assertEquals("Lambda default default_s 2", inlineFunWithDefaultParams())
  assertEquals("Lambda default non_default_s 2", inlineFunWithDefaultParams("non_default_s"))
  assertEquals(
    "Lambda default non_default_s 5",
    inlineFunWithDefaultParams(i = 5, s = "non_default_s"),
  )
  assertEquals(
    "non_default_s5",
    inlineFunWithDefaultParams("non_default_s", 5) { s: String, i: Int -> s + i },
  )
  assertTrue(inlineFunWithDefaultLambda<String>("String"))
  assertFalse(inlineFunWithDefaultLambda<Error>("String"))
  assertFalse(inlineFunWithDefaultLambda<String>("String") { false })
}

inline fun throwException(): Nothing = null!!

fun testReturnNothing() {
  assertThrowsNullPointerException { throwException() }
}

fun testNestedInlineFun() {
  var myClass = MyClass(5)
  val a =
    topLevelInlineFunction(myClass) { _ ->
      myClass.extensionInlineFunction {
        myClass.extensionInlineFunctionNoReturn { it - 2 }
        myClass.f
      }
    }
  // sum = 5 + 2 - 1
  assertEquals(a, 6)

  myClass = MyClass(5)
  val b =
    topLevelInlineFunction(myClass) { _ ->
      myClass.extensionInlineFunction {
        myClass.extensionInlineFunctionNoReturn {
          return@topLevelInlineFunction 100
        }
        myClass.f
      }
    }

  assertEquals(b, 100)
}

private fun callForEach(arr: IntArray): Int {
  arr.forEach {
    return it
  }
  return -1
}

fun testStdlibInlineFun() {
  assertTrue(1 == callForEach(intArrayOf(1, 2, 3)))
  assertTrue(-1 == callForEach(intArrayOf()))
  assertThrows(IllegalArgumentException::class.java) { require(false) }

  // flatMap is an inline function calling another inline function `flatMapTo` that have overloads
  // that conflict on the JVM. They resolve the conflict by using a JvmName annotation. We test that
  // we inline the right overload.
  assertTrue('F' == arrayOf("Foo").flatMap { it.toList() }[0])
  assertTrue('F' == arrayOf("Foo").flatMap { it.asSequence() }[0])

  // test inline extension property. `elementAtOrElse` is an extension inline function that use the
  // extension property `lastIndex`. This extension property is defined on different Types and this
  // test ensure that we resolve the right one.
  assertTrue(intArrayOf(1, 2, 3).elementAtOrElse(1) { -1 } == 2)
  assertTrue(intArrayOf(1, 2, 3).elementAtOrElse(5) { -1 } == -1)
}

fun testNonReifiedTypeParameterErasureInInlineFun() {
  // TODO(b/274670726): Revisit inconsistent semantics for non-reified type parameter in type
  // operations of inline functions.
  if (!isJvm()) {
    assertThrowsClassCastException {
      val a: Any = castToNonReifiedType<Int>(Any())
    }
  } else {
    val a: Any = castToNonReifiedType<Int>(Any())
  }

  assertEquals(2, callNonReifiedIntTypeParameterErasureInWhenInlineFun())
}

private inline fun <R> castToNonReifiedType(obj: Any): R = obj as R

private fun callNonReifiedIntTypeParameterErasureInWhenInlineFun(): Int =
  castToNonReifiedTypeInWhen<Int>() + 1

private inline fun <R> castToNonReifiedTypeInWhen(): R =
  // The if/else expression is important to ensure that there is no partial type erasure in the
  // entire expression leading to a type inconsistency that breaks the transpilation.
  if (false) 0 as R else 1 as R

interface Interface<T>

private inline fun inlineFunctionWithAnonymousObject() = object : Interface<String> {}

private inline fun <T> inlineFunctionWithTypeArgumentAndAnonymousObject() = object : Interface<T> {}

private inline fun <reified T> inlineFunctionWithReifiedTypeArgumentAndAnonymousObject() =
  object : Interface<T> {}

fun testInlinedLocalClassSemantics() {
  // Within a module, Kotlin/JVM shares the anonymous classes declared in inline functions between
  // all the call sites, while J2CL, like the other KLIB-based backends, copies them at each call
  // site.
  if (isJvm()) {
    assertTrue(
      inlineFunctionWithAnonymousObject()::class.java ===
        inlineFunctionWithAnonymousObject()::class.java
    )
    assertTrue(
      inlineFunctionWithTypeArgumentAndAnonymousObject<Int>()::class.java ===
        inlineFunctionWithTypeArgumentAndAnonymousObject<Int>()::class.java
    )
  } else {
    assertFalse(
      inlineFunctionWithAnonymousObject()::class.java ===
        inlineFunctionWithAnonymousObject()::class.java
    )
    assertFalse(
      inlineFunctionWithTypeArgumentAndAnonymousObject<Int>()::class.java ===
        inlineFunctionWithTypeArgumentAndAnonymousObject<Int>()::class.java
    )
  }
  // Even Kotlin/JVM copies the anonymous class at each call site when it uses a reified type
  // parameter. A reified type parameter has no runtime representation that could be passed to a
  // shared class, so the class has to be specialized with the type argument, e.g.
  // `object : Interface<T>` becomes `object : Interface<Int>`. The JVM inliner does this by
  // regenerating the class at each call site.
  assertFalse(
    inlineFunctionWithReifiedTypeArgumentAndAnonymousObject<Int>()::class.java ===
      inlineFunctionWithReifiedTypeArgumentAndAnonymousObject<Int>()::class.java
  )
  // When the inline function comes from another module, every backend, including Kotlin/JVM,
  // copies the anonymous class at each call site.
  assertFalse(
    inlineFunctionFromDepsWithAnonymousObject()::class.java ===
      inlineFunctionFromDepsWithAnonymousObject()::class.java
  )
}

interface ActionExecutor<P, R> {
  fun execute(param: P): R
}

inline fun <P, R> createActionExecutor(crossinline action: (P) -> R): ActionExecutor<P, R> {
  return object : ActionExecutor<P, R> {
    override fun execute(param: P): R = action(param)
  }
}

private fun testInlineFactoryFunctionWithCrossinlineLambda() {
  val intMultiplier = createActionExecutor<Int, Int> { value -> value * 3 }
  assertTrue(30 == intMultiplier.execute(10))

  var sideEffectVar = 0
  val sideEffectExecutor = createActionExecutor<Int, Unit> { value -> sideEffectVar = value + 5 }
  sideEffectExecutor.execute(10)

  assertTrue(sideEffectVar == 15)
}

inline fun inlineFunWithDefaults(a: Int, b: Int = 2): Int = a + b

inline fun inlineFunWithDefaultsAndVarargs(a: Int, b: Int = 2, vararg c: Int): Int = a + b + c.sum()

inline fun inlineFunWithDefaultsAndDefaultVarargs(
  a: Int,
  b: Int = 2,
  vararg c: Int = intArrayOf(3, 4, 5),
) = a + b + c.sum()

private fun testDefaultParams() {
  assertEquals(3, inlineFunWithDefaults(1))
  assertEquals(21, inlineFunWithDefaults(1, 20))

  assertEquals(3, inlineFunWithDefaultsAndVarargs(1))
  assertEquals(21, inlineFunWithDefaultsAndVarargs(1, 20))
  assertEquals(141, inlineFunWithDefaultsAndVarargs(1, 20, 30, 40, 50))

  assertEquals(15, inlineFunWithDefaultsAndDefaultVarargs(1))
  assertEquals(33, inlineFunWithDefaultsAndDefaultVarargs(1, 20))
  assertEquals(141, inlineFunWithDefaultsAndDefaultVarargs(1, 20, 30, 40, 50))
}

inline fun executeBlock(block: () -> Unit) = block()

fun testBreakContinue() {
  var i = 0
  while (true) {
    executeBlock {
      i++
      if (i < 10) {
        continue
      }
      break
    }
  }
  assertEquals(10, i)

  outer@ while (true) {
    executeBlock {
      while (true) {
        i++
        break@outer
      }
    }
  }
  assertEquals(11, i)
}

inline fun isSameVararg(vararg xs: Int): Boolean = xs === xs

inline fun writeInVararg(vararg xs: Int): Int {
  xs[0] = 42
  return xs[0]
}

private fun testInlineWithVararg() {
  assertTrue(isSameVararg(1, 2))
  assertEquals(42, writeInVararg(1, 2))
}

open class Box<T>(val value: T) {
  inline fun get(): T {
    val v: T = value
    return v
  }
}

open class SubBox<U>(u: U) : Box<U>(u)

private fun testProjectedReceiverTypeArgument() {
  // `T` is only known to be a supertype of `String` here, so the inlined body must not assume that
  // the value is a `String`.
  val box: Box<in String> = Box<Any>(42)
  assertTrue(42 == box.get())

  val subBox: SubBox<in String> = SubBox<Any>(42)
  assertTrue(42 == subBox.get())

  val outBox: Box<out Number> = Box<Int>(42)
  assertTrue(42 == outBox.get())

  val outSubBox: SubBox<out Number> = SubBox<Int>(42)
  assertTrue(42 == outSubBox.get())
}
