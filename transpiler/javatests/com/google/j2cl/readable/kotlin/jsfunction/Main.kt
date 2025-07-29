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
package jsfunction

import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsOptional
import jsinterop.annotations.JsOverlay
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

@JsFunction
fun interface Function<T, U> {
  fun apply(u: U): T
}

@JsFunction
fun interface JsFunctionInterface {
  fun foo(a: Int): Int

  @JsOverlay
  fun overlayMethod(): Int {
    return foo(42)
  }
}

class JsFunctionImplementation : JsFunctionInterface {
  var field = 0
  var storedThis: JsFunctionImplementation?
  var anotherStoredThis: JsFunctionImplementation?

  init {
    storedThis = this
    // Add an explicit unnecessary cast to make jscompiler happy.
    anotherStoredThis = this as JsFunctionImplementation?
  }

  fun bar(): Int {
    return 0
  }

  fun `fun`(): Int {
    field = 1
    return bar() + foo(1)
  }

  override fun foo(a: Int): Int {
    return a + bar() + field
  }
}

@JsMethod
fun createNativeFunction(): JsFunctionInterface {
  return null!!
}

fun callFn(fn: JsFunctionInterface, a: Int): Int {
  return fn.foo(a)
}

fun testJsFunction() {
  val func = JsFunctionImplementation()
  // call by Kotlin
  func.foo(10)
  // pass Javascript function to Kotlin.
  callFn(createNativeFunction(), 10)
  // call other instance methods and fields.
  val a = func.field
  func.bar()
}

fun testJsFunctionsCapturingLocal() {
  val n = 4
  // Use number as a variable to make sure it is aliased properly.
  callFn(JsFunctionInterface { number: Int -> number + n }, n)
  callFn(
    object : JsFunctionInterface {
      override fun foo(a: Int): Int {
        return a + n
      }
    },
    n,
  )
  object : JsFunctionInterface {
      override fun foo(a: Int): Int {
        instanceMethod()
        return 0
      }
    }
    .foo(3)
}

fun testJsFunctionThis() {
  object : JsFunctionInterface {
      override fun foo(a: Int): Int {
        // captures this
        instanceMethod()
        return 0
      }
    }
    .foo(3)
}

private fun instanceMethod() {}

interface Container<T> {
  fun get(): T?
}

class ContainerImpl<T> : Container<T> {
  override fun get(): T? = null
}

fun testJsFunctionErasureCasts() {
  val container: Container<Function<String, String>> = ContainerImpl()
  acceptsJsFunction(container.get())
}

fun acceptsJsFunction(f: Function<String, String>?) {}

internal class TestCaptureOuterParametricClass<T> {
  fun test() {
    val f: Function<Any, Any> = Function<Any, Any> { `object`: Any? -> Any() }
  }
}

@JsFunction
fun interface JsFunctionVarargs {
  fun m(i: Int, vararg numbers: Int): Int
}

fun testJsFunctionVarargs(): JsFunctionVarargs {
  return JsFunctionVarargs { i: Int, numbers: IntArray ->
    var sum: Int = i
    for (number in numbers) {
      sum += number
    }
    sum
  }
}

fun testJsFunctionVarargsInnerClass(): JsFunctionVarargs {
  return object : JsFunctionVarargs {
    override fun m(i: Int, vararg numbers: Int): Int {
      var sum = i
      for (number in numbers) {
        sum += number
      }
      return sum
    }
  }
}

@JsFunction
fun interface ForEachCallBack<T> {
  fun onInvoke(p0: T, p1: Int, p2: Array<out T?>?): Any?
}

@JsFunction
fun interface VaragsJsFunction {
  fun call(vararg args: Any?): Any?
}

fun testVarArgsMethodReferenceToJsFuncion() {
  // Kotlin does not allow using varags to implement a functional interface unless it matches the
  // underlying array.
  // val c: ForEachCallBack<VaragsJsFunction?> =
  //   ForEachCallBack<VaragsJsFunction?> (VaragsJsFunction::call)
}

@JsFunction
fun interface JsFunctionVarargsGenerics<T> {
  fun m(i: Int, vararg numbers: T): Int
}

fun <T> acceptsVarargsJsFunctionWithTypeVariable(x: JsFunctionVarargsGenerics<T>?) {}

fun <T> acceptsVarargsJsFunctionWithParemetrizedType(x: JsFunctionVarargsGenerics<Array<T>?>?) {}

@JsMethod
fun <T> acceptsVarargsJsFunctionWithTypeVariableInVarargs(
  vararg x: JsFunctionVarargsGenerics<T>?
) {}

@JsMethod
fun <T> acceptsVarargsJsFunctionWithParemetrizedTypeInVarargs(
  vararg x: JsFunctionVarargsGenerics<Array<T>?>?
) {}

@JsFunction
fun interface SimpleJsFunction {
  fun m()
}

@JsMethod fun acceptsJsFunctionInVarargs(vararg x: SimpleJsFunction?) {}

fun testJsFunctionClassLiterals() {
  val array = arrayOf<SimpleJsFunction>()
  var o: Any = SimpleJsFunction::class.java
  o = Array<SimpleJsFunction>::class.java
}

@JsFunction
internal fun interface JsFunctionOptional {
  fun m(i: Int, @JsOptional number: Double?): Int
}

internal class JsFunctionOptionalImpl : JsFunctionOptional {
  override fun m(i: Int, @JsOptional number: Double?): Int {
    return (i + number!!).toInt()
  }
}

fun testJsFunctionOptional() {
  val f: JsFunctionOptional = JsFunctionOptional { i: Int, n: Double? -> (i + n!!).toInt() }
}

@JsFunction
fun interface ParametricJsFunction<E> {
  fun call(event: E)
}

var jsFunctionFieldWildcard: ParametricJsFunction<*> = ParametricJsFunction<Any> { event: Any? -> }
var jsFunctionFieldParameterized: ParametricJsFunction<String> =
  ParametricJsFunction<String> { event: String? -> }

internal interface ApiWithMethodReturningParametricJsFunction {
  fun <T> anApi(): ParametricJsFunction<T>?
}

internal class Implementor : ApiWithMethodReturningParametricJsFunction {
  @JsMethod
  override fun <T> anApi(): ParametricJsFunction<T>? {
    return null as ParametricJsFunction<T>?
  }
}

internal class ParametricImplementation<T> : ParametricJsFunction<T> {
  override fun call(t: T) {
    val o: Any = t!!
  }
}

@JsProperty private fun setParametricJsFunction(fn: ParametricJsFunction<Any>?) {}

private val parametricJsFunction: ParametricJsFunction<Any>?
  @JsProperty private get() = null

fun testFunctionExpressionTypeReplacement() {
  val f: ParametricJsFunction<String> =
    ParametricJsFunction<String> { unused: String? ->
      val l: Array<Array<*>?> = arrayOfNulls(1)
      l[0] = arrayOf("")
    }
}

private class ClassWithJsFunctionProperty {
  var fn: ParametricJsFunction<String>? = null

  @JsProperty
  fun getFunction(): ParametricJsFunction<String>? {
    return null
  }
}

fun testJsFunctionPropertyCall() {
  val c = ClassWithJsFunctionProperty()
  c.fn!!.call("")
  c.getFunction()!!.call("")
  c.fn!!.call("")
  (if (c != null) c.fn else null)!!.call("")
}

@JsFunction
fun interface JsBiFunction<T, S : Number?> {
  fun apply(t: T, s: S): T
}

class DoubleDoubleJsBiFunction : JsBiFunction<Double?, Double?> {
  override fun apply(d: Double?, i: Double?): Double? {
    return d
  }
}

class TIntegerJsBiFunction<T> : JsBiFunction<T?, Int?> {
  override fun apply(element: T?, i: Int?): T? {
    return null
  }
}

@JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
internal class TestJsFunctionInJsOverlayCapturingOuter {
  @JsOverlay
  fun test() {
    sort(
      JsFunctionInterface { a: Int ->
        if (this@TestJsFunctionInJsOverlayCapturingOuter == null) 0 else 1
      }
    )
  }

  @JsOverlay fun sort(func: JsFunctionInterface?) {}
}

// TODO(b/233615397): Verify that the errors in the build log are gone after fixing this issue.
fun callInterfaceProjected(f: JsBiFunction<*, *>, o: Any?, n: Number?): Any? {
  return (f as JsBiFunction<Any?, Number?>).apply(o, n)
}

fun callInterfaceParameterized(f: JsBiFunction<String?, Int?>, s: String?): String? {
  return f.apply(s, 1)
}

fun <U, V : Number?> callInterfaceUnparameterized(f: JsBiFunction<U, V>, u: U, v: V): U {
  return f.apply(u, v)
}

fun callImplementorPorjected(f: TIntegerJsBiFunction<*>, o: Any?, n: Int?): Any? {
  return (f as TIntegerJsBiFunction<Any?>).apply(o, n)
}

fun callImplementorParameterized(f: TIntegerJsBiFunction<String?>, s: String?): String? {
  return f.apply(s, 1)
}

fun testParameterTypes() {
  val tIntegerJsBiFunction: JsBiFunction<String?, Int?> = TIntegerJsBiFunction<String>()
  val doubleDoubleJsBiFunction: JsBiFunction<Double?, Double?> = DoubleDoubleJsBiFunction()
  callInterfaceProjected(tIntegerJsBiFunction, "a", 1)
  callInterfaceProjected(doubleDoubleJsBiFunction, 1.1, 1.1)
  callInterfaceParameterized(tIntegerJsBiFunction, "a")
  callInterfaceUnparameterized(tIntegerJsBiFunction, "a", 1)
  callInterfaceUnparameterized(doubleDoubleJsBiFunction, 1.1, 1.1)
  callImplementorPorjected(TIntegerJsBiFunction<Double>(), 1.1, 1)
  callInterfaceUnparameterized<String?, Int?>(tIntegerJsBiFunction, "a", 1)
  callInterfaceUnparameterized<Double?, Double?>(doubleDoubleJsBiFunction, 1.1, 1.1)
  callImplementorParameterized(TIntegerJsBiFunction(), "")
  tIntegerJsBiFunction.apply("a", 1)
  doubleDoubleJsBiFunction.apply(1.1, 1.1)
  callOnFunction(DoubleDoubleJsBiFunction())
}

@JsMethod
fun callOnFunction(f: JsBiFunction<Double?, Double?>?): Double {
  return 0.0
}

fun testCast() {
  val o: Any = TIntegerJsBiFunction<String>()
  val rawTIntegerJsBiFunction = o as TIntegerJsBiFunction<*>
  val parameterizedTIntegerJsBiFunction = o as TIntegerJsBiFunction<String>
  val anotherRawJsBiFunction = o as JsBiFunction<*, *>
  val anotherParameterizedJsBiFunction = o as JsBiFunction<String, Int>
  val doubleDoubleJsBiFunction = o as DoubleDoubleJsBiFunction
}

fun testNewInstance() {
  val rawTIntegerJsBiFunction: TIntegerJsBiFunction<*> = TIntegerJsBiFunction<Any?>()
  val parameterizedTIntegerJsBiFunction: TIntegerJsBiFunction<String> =
    TIntegerJsBiFunction<String>()
  val rawJsBiFunction: JsBiFunction<*, *> = DoubleDoubleJsBiFunction()
}

private class RecursiveParametricJsFunctionImplementation<T : ParametricJsFunction<T>?> :
  ParametricJsFunction<T> {
  override fun call(t: T) {}
}

private class RecursiveJsFunctionImplementation :
  ParametricJsFunction<RecursiveJsFunctionImplementation?> {
  override fun call(t: RecursiveJsFunctionImplementation?) {}
}
