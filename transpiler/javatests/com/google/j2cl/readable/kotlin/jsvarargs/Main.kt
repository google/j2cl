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
package jsvarargs

import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType
import jsvarargs.Main.GenericFunction

open class Main(f: Int) {
  val field = f

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  internal open class NativeObject internal constructor(vararg pars: Any?)

  internal class SubVarargsConstructorClass @JsConstructor constructor(i: Int, vararg args: Any?) :
    NativeObject(args)

  @JsFunction
  fun interface Function {
    fun f1(i: Int, vararg args: Any?): Any?
  }

  @JsType
  abstract class AbstractMethodWithVarargs {
    abstract fun abstractMethod(vararg args: Int)
  }

  @JsType
  interface StaticInterfaceMethodWithVarargs {
    companion object {
      @JvmStatic fun staticMethod(vararg args: Int) {}
    }
  }

  internal class AFunction : Function {
    override fun f1(i: Int, vararg args: Any?): Any? {
      return args[i]
    }
  }

  internal class SubMain : Main(10) {
    override fun f3(m: Int, vararg numbers: Int): Int {
      // multiple arguments.
      var a = super.f3(1, 1, 2)
      // no argument for varargs.
      a += super.f3(1)
      // array spread for varargs.
      a += super.f3(1, *intArrayOf(1, 2))
      // empty array spread for varargs.
      a += super.f3(1, *intArrayOf())
      // array object spread for varargs.
      val ints = intArrayOf(1, 2)
      a += super.f3(1, *ints)
      return a
    }
  }

  // instance JsMethod, with varargs that is not the first parameter.
  @JsMethod
  open fun f3(m: Int, vararg numbers: Int): Int {
    return this.field + m + numbers[1]
  }

  // instance JsMethod, with varargs that is the first parameter.
  @JsMethod
  fun f4(vararg numbers: Int): Int {
    return this.field + numbers[1]
  }

  companion object {
    // static JsMethod, with varargs that is not the first parameter.
    @JsMethod
    @JvmStatic
    fun f1(multiplier: Int, vararg numbers: Int): Int {
      return numbers.size + numbers[0] + multiplier
    }

    // static JsMethod, with varargs that is the first parameter.
    @JsMethod
    @JvmStatic
    fun f2(vararg numbers: Int): Int {
      return numbers.size + numbers[0]
    }

    @JsMethod
    @JvmStatic
    fun <T> generics(vararg elements: T): T {
      return elements[0]
    }

    @JsMethod
    @JvmStatic
    fun parameterizedType(vararg elements: List<Main>): Main {
      return elements[0][0]
    }

    @JsMethod
    @JvmStatic
    fun <T> parameterizedByT(vararg elements: List<T>): T {
      return elements[0][0]
    }
  }

  fun testStaticMethodNotFirst() {
    // multiple arguments.
    f1(1, 1, 2)
    Main.f1(1, 1, 2)
    // no argument for varargs.
    f1(1)
    Main.f1(1)
    // array spread for varargs.
    f1(1, *intArrayOf(1, 2))
    Main.f1(1, *intArrayOf(1, 2))
    // empty array spread for varargs.
    f1(1, *intArrayOf())
    Main.f1(1, *intArrayOf())
    // array object spread for varargs.
    val ints = intArrayOf(1, 2)
    f1(1, *ints)
    // In Kotlin varargs aren't also representable by an array, so null is not acceptable.
    // f1(1, null)
  }

  fun testStaticMethodFirst() {
    // multiple arguments.
    f2(1, 2)
    generics(1, 2)
    f2(1, 2)
    generics(1, 2)
    // no argument for varargs.
    f2()
    generics<Any?>()
    f2()
    generics<Any?>()
    generics<Int>()
    // array spread for varargs.
    f2(*intArrayOf(1, 2))
    f2(*intArrayOf(1, 2))
    generics(*arrayOf(1, 2))
    generics(*arrayOf(1, 2))
    // empty array spread for varargs.
    f2(*intArrayOf())
    f2(*intArrayOf())
    generics(emptyArray<Int>())
    // array object for varargs.
    val ints = intArrayOf(1, 2)
    val integers = arrayOf(1, 2)
    f2(*ints)
    f2(*ints)
    generics(integers)
  }

  fun testInstanceMethodNotFirst() {
    val m = Main(1)
    // multiple arguments.
    m.f3(1, 1, 2)
    // no argument for varargs.
    m.f3(1)
    // array spread for varargs.
    m.f3(1, *intArrayOf(1, 2))
    // empty array spread for varargs.
    m.f3(1, *intArrayOf())
    // array object spread for varargs.
    val ints = intArrayOf(1, 2)
    m.f3(1, *ints)
  }

  fun testInstanceMethodFirst() {
    val m = Main(1)
    // multiple arguments.
    m.f4(1, 2)
    // no argument for varargs.
    m.f4()
    // array spread for varargs.
    m.f4(*intArrayOf(1, 2))
    // empty array spread for varargs.
    m.f4(*intArrayOf())
    // array object spread for varargs.
    val ints = intArrayOf(1, 2)
    m.f4(*ints)
  }

  fun testJsFunction() {
    val a = AFunction()
    val o1 = Any()
    val o2 = Any()
    // multiple arguments.
    a.f1(0, o1, o2)
    // no argument for varargs.
    a.f1(0)
    // array spread for varargs.
    a.f1(0, *arrayOf(o1, o2))
    // empty array spread for varargs.
    a.f1(0, *arrayOf())
    a.f1(0, *emptyArray())
    // array object spread for varargs.
    val os = arrayOf(o1, o2)
    a.f1(0, *os)
  }

  fun testSideEffect() {
    val ints = intArrayOf(1, 2)
    Main(1).f3(1, *ints)
  }

  // In Kotlin varargs aren't also representable by an array, so null is not acceptable.
  // fun testNullJsVarargs() {
  //   val ints: Array<Int>? = null
  //   f2(ints)
  // }

  @JsFunction
  fun interface GenericFunction<T> {
    fun m(i: T, vararg args: T): Any?
  }

  fun <U> testGenericJsFunctionWithVarags() {
    val function = GenericFunction<U> { n, param -> param }
  }
}
