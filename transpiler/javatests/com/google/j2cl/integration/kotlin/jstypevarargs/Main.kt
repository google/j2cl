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
package jstypevarargs

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsMethod

fun main(vararg args: String) {
  testInstanceMethodFirst()
  testInstanceMethodNotFirst()
  testStaticMethodFirst()
  testStaticMethodNotFirst()
  testJsFunction()
  testSideEffect()
  testSuperMethodCall()
  // This test doesn't make sense in Kotlin as arrays cannot be pass in place of varargs. Instead
  // they need to be explicitly spread, which cannot be applied to nullable types.
  // testCallVarargsWithNull()
  testUnboxedType()
  QualifiedSuperMethodCall.test()
}

open class Main {

  @JsFunction
  fun interface Function {
    fun f1(i: Int, vararg args: Main?): Int
  }

  class AFunction : Function {
    override fun f1(i: Int, vararg args: Main?): Int {
      if (i >= args.size) {
        return -1
      }
      return args[i]?.field ?: -1
    }
  }

  class SubMain(x: Int, y: Int) : Main(x + y) {
    // multiple arguments.
    fun test1(): Int = super.f3(10, 1, 2)

    // no argument for varargs.
    fun test2(): Int = super.f3(10)

    // array literal for varargs.
    fun test3(): Int = super.f3(10, *intArrayOf(1, 2))

    // empty array literal for varargs.
    fun test4(): Int = super.f3(10, *intArrayOf())

    // array object for varargs.
    fun test5(): Int {
      val ints = intArrayOf(1, 2)
      return super.f3(10, *ints)
    }
  }

  internal var field: Int

  constructor(f: Int) {
    this.field = f
  }

  fun sideEffect(a: Int): Main {
    this.field += a
    return this
  }

  companion object {
    // varargs of unboxed type.
    @JsMethod
    @JvmStatic
    fun sumAndMultiply(multiplier: Double, vararg numbers: Double): Double {
      var result = 0.0
      for (d in numbers) {
        result += d
      }
      result *= multiplier
      return result
    }

    // static JsMethod, with varargs that is not the first parameter.
    @JsMethod
    @JvmStatic
    fun f1(m: Int, vararg numbers: Int): Int {
      var result = 0
      for (n in numbers) {
        result += n
      }
      result *= m
      return result
    }

    // static JsMethod, with varargs that is the first parameter.
    @JsMethod
    @JvmStatic
    fun f2(vararg numbers: Int): Int {
      var result = 0
      for (n in numbers) {
        result += n
      }
      result *= 100
      return result
    }

    @JsMethod @JvmStatic fun <T> generics(vararg elements: T): T = elements[0]
  }

  // instance JsMethod, with varargs that is not the first parameter.
  @JsMethod
  fun f3(m: Int, vararg numbers: Int): Int {
    var result = 0
    for (n in numbers) {
      result += n
    }
    result += field
    result *= m
    return result
  }

  // instance JsMethod, with varargs that is the first parameter.
  @JsMethod
  fun f4(vararg numbers: Int): Int {
    var result = 0
    for (n in numbers) {
      result += n
    }
    result += field
    result *= 100
    return result
  }
}

fun testUnboxedType() {
  // multiple arguments.
  assertTrue(Main.sumAndMultiply(10.0, 1.0, 2.0) == 30.0)
  assertTrue(Main.Companion.sumAndMultiply(10.0, 1.0, 2.0) == 30.0)
  // no argument for varargs.
  assertTrue(Main.sumAndMultiply(10.0) == 0.0)
  assertTrue(Main.Companion.sumAndMultiply(10.0) == 0.0)
  // array literal for varargs.
  assertTrue(Main.sumAndMultiply(10.0, *doubleArrayOf(1.0, 2.0)) == 30.0)
  assertTrue(Main.Companion.sumAndMultiply(10.0, *doubleArrayOf(1.0, 2.0)) == 30.0)
  // empty array literal for varargs.
  assertTrue(Main.sumAndMultiply(10.0, *doubleArrayOf()) == 0.0)
  assertTrue(Main.Companion.sumAndMultiply(10.0, *doubleArrayOf()) == 0.0)
  // array object for varargs.
  val ds = doubleArrayOf(1.0, 2.2)
  assertTrue(Main.sumAndMultiply(10.0, *ds) == 32.0)
  assertTrue(Main.Companion.sumAndMultiply(10.0, *ds) == 32.0)
  // call by JS.
  assertTrue(callSumAndMultiply() == 30.0)
}

fun testStaticMethodNotFirst() {
  // multiple arguments.
  assertTrue(Main.f1(10, 1, 2) == 30)
  assertTrue(Main.Companion.f1(10, 1, 2) == 30)
  // no argument for varargs.
  assertTrue(Main.f1(10) == 0)
  assertTrue(Main.Companion.f1(10) == 0)
  // array literal for varargs.
  assertTrue(Main.f1(10, *intArrayOf(1, 2)) == 30)
  assertTrue(Main.Companion.f1(10, *intArrayOf(1, 2)) == 30)
  // empty array literal for varargs.
  assertTrue(Main.f1(10, *intArrayOf()) == 0)
  assertTrue(Main.Companion.f1(10, *intArrayOf()) == 0)
  // array object for varargs.
  val ints = intArrayOf(1, 2)
  assertTrue(Main.f1(10, *ints) == 30)
  assertTrue(Main.Companion.f1(10, *ints) == 30)
  // call by JS.
  assertTrue(callF1() == 30)
}

fun testStaticMethodFirst() {
  // multiple arguments.
  assertTrue(Main.f2(1, 2) == 300)
  assertTrue(Main.Companion.f2(1, 2) == 300)
  // no argument for varargs.
  assertTrue(Main.f2() == 0)
  assertTrue(Main.Companion.f2() == 0)
  // array literal for varargs.
  assertTrue(Main.f2(*intArrayOf(1, 2)) == 300)
  assertTrue(Main.Companion.f2(*intArrayOf(1, 2)) == 300)
  // empty array literal for varargs.
  assertTrue(Main.f2(*intArrayOf()) == 0)
  assertTrue(Main.Companion.f2(*intArrayOf()) == 0)
  // array object for varargs.
  val ints = intArrayOf(1, 2)
  assertTrue(Main.f2(*ints) == 300)
  assertTrue(Main.Companion.f2(*ints) == 300)
  // call by JS.
  assertTrue(callF2() == 300)
  assertTrue(1 == Main.generics(1, 2))
  assertTrue("abc".equals(Main.Companion.generics("abc", "def")))
}

fun testInstanceMethodNotFirst() {
  val m = Main(1)
  // multiple arguments.
  assertTrue(m.f3(10, 1, 2) == 40)
  // no argument for varargs.
  assertTrue(m.f3(10) == 10)
  // array literal for varargs.
  assertTrue(m.f3(10, *intArrayOf(1, 2)) == 40)
  // empty array literal for varargs.
  assertTrue(m.f3(10, *intArrayOf()) == 10)
  // array object for varargs.
  val ints = intArrayOf(1, 2)
  assertTrue(m.f3(10, *ints) == 40)
  // call by JS.
  assertTrue(callF3(m) == 40)
}

fun testInstanceMethodFirst() {
  val m = Main(1)
  // multiple arguments.
  assertTrue(m.f4(1, 2) == 400)
  // no argument for varargs.
  assertTrue(m.f4() == 100)
  // array literal for varargs.
  assertTrue(m.f4(*intArrayOf(1, 2)) == 400)
  // empty array literal for varargs.
  assertTrue(m.f4(*intArrayOf()) == 100)
  // array object for varargs.
  val ints = intArrayOf(1, 2)
  assertTrue(m.f4(*ints) == 400)
  // call by JS.
  assertTrue(callF4(m) == 400)
}

fun testJsFunction() {
  val a = Main.AFunction()
  val m1 = Main(12)
  val m2 = Main(34)
  // multiple arguments.
  assertTrue(a.f1(1, m1, m2) == 34)
  // no argument for varargs.
  assertTrue(a.f1(1) == -1)
  // array literal for varargs.
  assertTrue(a.f1(0, *arrayOf(m1, m2)) == 12)
  // empty array literal for varargs.
  assertTrue(a.f1(0, *emptyArray<Main>()) == -1)
  // array object for varargs.
  val os = arrayOf(m1, m2)
  assertTrue(a.f1(1, *os) == 34)
  // call by JS
  assertTrue(callJsFunction(a) == -1)
}

fun testSideEffect() {
  val m = Main(10)
  assertTrue(m.field == 10)
  val ints = intArrayOf(1, 2)
  m.sideEffect(5).f3(10, *ints)
  assertTrue(m.field == 15)
}

fun testSuperMethodCall() {
  val sm = Main.SubMain(1, 0)
  assertTrue(sm.test1() == 40)
  assertTrue(sm.test2() == 10)
  assertTrue(sm.test3() == 40)
  assertTrue(sm.test4() == 10)
  assertTrue(sm.test5() == 40)
}

@JsMethod private fun count(vararg strings: String): Int = strings.size

// This test doesn't make sense in Kotlin as arrays cannot be pass in place of varargs. Instead they
// need to be explicitly spread, which cannot be applied to nullable types.
// fun testCallVarargsWithNull() {
//   assertTrue(count("Hello") == 1)
//   try {
//     val strings: Array<String>? = null
//     assertTrue(count(*strings) == 0)
//     assertTrue(false)
//   } catch (expected: Exception) {
//     return
//   }
//   assertTrue(false)
// }

private open class IntegerSummarizer {
  fun summarize(vararg values: Int?): Int {
    var sum = 0
    for (value in values) {
      sum += value!!
    }
    return sum
  }
}

interface Summarizer<T> {
  @JsMethod fun summarize(vararg values: T): Int
}

private class SummarizerImplementor : IntegerSummarizer(), Summarizer<Int?> {}

fun testBridgeWithVarargs() {
  assertEquals(6, SummarizerImplementor().summarize(1, 2, 3))
  val rawSummarizer: Summarizer<Int?> = SummarizerImplementor()
  // Simulate a call from JS with an untyped array.
  assertEquals(6, rawSummarizer.summarize(1, 2, 3))
}

@JsMethod private external fun callSumAndMultiply(): Double

@JsMethod private external fun callF1(): Int

@JsMethod private external fun callF2(): Int

@JsMethod private external fun callF3(m: Main): Int

@JsMethod private external fun callF4(m: Main): Int

@JsMethod private external fun callJsFunction(a: Main.AFunction): Int
