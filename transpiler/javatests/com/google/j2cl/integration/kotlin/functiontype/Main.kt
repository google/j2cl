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
package functiontype

import com.google.j2cl.integration.testing.Asserts.assertNull
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testLambda()
  testTypeHierarchy()
  testFunctionReference()
  testNestedFunctionType()
  testNullableFunctionType()
  testGenericFunctionType()
  testBigArityFunctionType()
}

fun funWithIntFunctionType(f: () -> Int) = f()

fun funWithIntIntFunctionType(f: (Int) -> Int) = f(2)

fun funWithIntAnyFunctionType(f: (Int) -> Any) = f(3)

fun funWithExtensionFunctionType(subSequenceImpl: String.(Int, Int) -> CharSequence) =
  "123".subSequenceImpl(1, 2)

fun testLambda() {
  assertTrue(1 == funWithIntFunctionType { 1 })
  assertTrue(2 == funWithIntIntFunctionType { it })
  assertTrue(4 == funWithIntIntFunctionType { it * 2 })

  var subSequenceFun: (String) -> CharSequence = { it.subSequence(0, 1) }
  assertTrue("1" == subSequenceFun("123"))
  subSequenceFun = { foo: CharSequence -> foo.subSequence(0, 1) }
  assertTrue("1" == subSequenceFun("123"))

  assertTrue("2" == funWithExtensionFunctionType { s, e -> this.subSequence(s, e) })

  // specify a different return type
  assertTrue(
    3 ==
      funWithIntAnyFunctionType(
        fun(n: Number): Number {
          return n
        }
      )
  )
}

class PowerTwo : (Int) -> Int {
  override operator fun invoke(x: Int): Int = x * x
}

class IntegralPowerTwoForNumber : (Number) -> Number {
  override operator fun invoke(x: Number): Int = x.toInt() * x.toInt()
}

class ImplementsMultipleFunctionType : (Int) -> Any, (String, String) -> Any {
  override operator fun invoke(x: Int): Any = x + 2

  override operator fun invoke(p1: String, p2: String): Any = "$p1 $p2"
}

fun testTypeHierarchy() {
  assertTrue(4 == funWithIntIntFunctionType(PowerTwo()))
  assertTrue(9 == funWithIntAnyFunctionType(PowerTwo()))
  assertTrue(9 == funWithIntAnyFunctionType(IntegralPowerTwoForNumber()))

  val intToAny: (Int) -> Any = ImplementsMultipleFunctionType()
  assertTrue(5 == intToAny(3))
  val stringStringToAny: (String, String) -> Any = ImplementsMultipleFunctionType()
  assertTrue("Hello World" == stringStringToAny("Hello", "World"))
}

fun testFunctionReference() {
  fun funTakingIntReturningInt(i: Int): Int = i

  assertTrue(2 == funWithIntIntFunctionType(::funTakingIntReturningInt))
  assertTrue("2" == funWithExtensionFunctionType(CharSequence::subSequence))

  fun funWithStringStringFunctionType(f: (String, String) -> String): String {
    return f("Foo", "Bar")
  }

  fun withStringVararg(vararg s: String): String {
    return s.joinToString()
  }

  var varargsFctRef = ::withStringVararg
  assertTrue("Foo, Bar, Baz" == varargsFctRef.invoke(arrayOf("Foo", "Bar", "Baz")))

  // You cannot use varagsFctRef in this case below. It's a compilation error: it expects a
  // `Function2<String, String, String>` and `varargsFctRef` is typed as
  // `Function1<Array<String>>, String>`. When you directly use the function reference, Kotlin
  // compiler will produce a function adapter that takes two String as parameter and forward the
  // call to the varargs function.
  assertTrue("Foo, Bar" == funWithStringStringFunctionType(::withStringVararg))
}

fun testNestedFunctionType() {
  fun funWithNestedFunctionType(f: (() -> Int) -> ((Int) -> Int)): Int {
    return f.invoke { 2 }.invoke(2)
  }

  assertTrue(4 == funWithNestedFunctionType { f -> { i -> f() * i } })
}

fun testNullableFunctionType() {
  fun funWithNullableFunctionType(f: ((Int) -> Int)?): Int? {
    return f?.invoke(2)
  }

  assertNull(funWithNullableFunctionType(null))
  assertTrue(2 == funWithNullableFunctionType { it })
}

fun testGenericFunctionType() {
  fun <T, R> funWithGenericFunctionType(t: T, f: (T) -> R): R {
    return f(t)
  }

  assertTrue("2" == funWithGenericFunctionType(2) { it.toString() })
  assertTrue("Hello World" == funWithGenericFunctionType("World") { "Hello $it" })
}

fun testBigArityFunctionType() {
  // When arity <= 22, function typeis converted to koltin.jvm.functions.Function{arity}
  fun funWith22ParametersFunctionType(
    f:
      (
        p1: String,
        p2: String,
        p3: String,
        p4: String,
        p5: String,
        p6: String,
        p7: String,
        p8: String,
        p9: String,
        p10: String,
        p11: String,
        p12: String,
        p13: String,
        p14: String,
        p15: String,
        p16: String,
        p17: String,
        p18: String,
        p19: String,
        p20: String,
        p21: String,
        p22: String,
      ) -> String
  ): String {
    return f(
      "1",
      "2",
      "3",
      "4",
      "5",
      "6",
      "7",
      "8",
      "9",
      "10",
      "11",
      "12",
      "13",
      "14",
      "15",
      "16",
      "17",
      "18",
      "19",
      "20",
      "21",
      "22",
    )
  }

  assertTrue(
    "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22" ==
      funWith22ParametersFunctionType {
        p1,
        p2,
        p3,
        p4,
        p5,
        p6,
        p7,
        p8,
        p9,
        p10,
        p11,
        p12,
        p13,
        p14,
        p15,
        p16,
        p17,
        p18,
        p19,
        p20,
        p21,
        p22 ->
        "$p1 $p2 $p3 $p4 $p5 $p6 $p7 $p8 $p9 $p10 $p11 $p12 $p13 $p14 $p15 $p16 $p17 $p18 $p19 $p20 $p21 $p22"
      }
  )

  // TODO(b/258286507): Enable when the mapping to FunctionN vararg is supported.
  // // Above 22, the function type is mapped to kotlin.jvm.functions.FunctionN with varargs
  // fun funWith23ParametersFunctionType(
  //   f:
  //     (
  //     p1: String,
  //     p2: String,
  //     p3: String,
  //     p4: String,
  //     p5: String,
  //     p6: String,
  //     p7: String,
  //     p8: String,
  //     p9: String,
  //     p10: String,
  //     p11: String,
  //     p12: String,
  //     p13: String,
  //     p14: String,
  //     p15: String,
  //     p16: String,
  //     p17: String,
  //     p18: String,
  //     p19: String,
  //     p20: String,
  //     p21: String,
  //     p22: String,
  //     p23: String
  //   ) -> String
  // ): String {
  //   return f(
  //     "1",
  //     "2",
  //     "3",
  //     "4",
  //     "5",
  //     "6",
  //     "7",
  //     "8",
  //     "9",
  //     "10",
  //     "11",
  //     "12",
  //     "13",
  //     "14",
  //     "15",
  //     "16",
  //     "17",
  //     "18",
  //     "19",
  //     "20",
  //     "21",
  //     "22",
  //     "23"
  //   )
  // }
  //
  // assertTrue(
  //   "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23" ==
  //     funWith23ParametersFunctionType {
  //         p1,
  //         p2,
  //         p3,
  //         p4,
  //         p5,
  //         p6,
  //         p7,
  //         p8,
  //         p9,
  //         p10,
  //         p11,
  //         p12,
  //         p13,
  //         p14,
  //         p15,
  //         p16,
  //         p17,
  //         p18,
  //         p19,
  //         p20,
  //         p21,
  //         p22,
  //       p23 ->
  //       "$p1 $p2 $p3 $p4 $p5 $p6 $p7 $p8 $p9 $p10 $p11 $p12 $p13 $p14 $p15 $p16 $p17 $p18 $p19
  // $p20 $p21 $p22 $p23"
  //     }
  // )
}
