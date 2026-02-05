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
package lambdas

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertNotEquals
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.TestUtils.isJvm

fun main(vararg unused: String) {
  val captures = Captures()
  captures.testLambdaNoCapture()
  captures.testInstanceofLambda()
  captures.testLambdaCaptureField()
  captures.testCapturedVariableScoping()
  captures.testLambdaCaptureLocal()
  captures.testLambdaCaptureFieldAndLocal()
  // fun interfaces must have exactly one abstract method and SubEquals becomes a fun interface if
  // SubEquals extends Equals in Kotlin
  // testSpecialLambdas()
  testSpecializedLambda()
  testVarargsLambdas()
  // kotlin does not use var in lambda arguments
  // testVarKeywordInLambda()
  // Kotlin lambda are automatically Serializable but koltin.io.Serializable is restricted
  // testSerializableLambda()
  testNestedLambdas()
  testArbitraryNesting()
  // Kotlin does not support intersection types
  // testIntersectionTypeLambdas()
  testInStaticContextWithInnerClasses()
}

private fun interface IntToIntFunction {
  fun apply(i: Int): Int
}

private class Captures {
  private val field = 100

  private fun test(f: IntToIntFunction, n: Int): Int {
    return field + f.apply(n)
  }

  internal fun testLambdaNoCapture() {
    var result = test(IntToIntFunction { i: Int -> i + 1 }, 10)
    assertTrue(result == 111)
    result =
      test(
        IntToIntFunction { i: Int ->
          object {
              var storedValue = i

              fun addTwo() = this.storedValue + 2
            }
            .addTwo()
        },
        10,
      )
    assertTrue(result == 112)
  }

  internal fun testInstanceofLambda() {
    val f = IntToIntFunction { i: Int -> i + 1 }
    assertTrue(f is IntToIntFunction)
  }

  internal fun testLambdaCaptureField() {
    val result = test(IntToIntFunction { i: Int -> field + i + 1 }, 10)
    assertTrue(result == 211)

    class Local {
      var field = 10

      inner class Inner {
        fun getOuterField(): Int {
          return this@Local.field
        }
      }
    }

    assertEquals(10, Local().Inner().getOuterField())
  }

  internal fun testLambdaCaptureLocal() {
    var x = 1
    val result =
      test(
        IntToIntFunction { i: Int ->
          x = 10
          x + i + 1
        },
        10,
      )
    assertTrue(result == 121)
    assertTrue(x == 10)
  }

  fun interface IntSupplier {
    fun get(): Int
  }

  internal fun testCapturedVariableScoping() {
    val suppliers = mutableListOf<IntSupplier>()

    fun captureAndReturn(intSupplier: IntSupplier): Int {
      suppliers.add(intSupplier)
      return intSupplier.get()
    }

    var x = 0
    do {
      var i = x++
      // The variable `i` is captured by the Supplier lambda.
      suppliers.add { i }
      // and modified here, which should be reflected in the lambda above when
      // evaluating.
      i++
      if (x == 1) {
        continue
      }
      // This modification will be seen only in the second supplier.
      i++
      // Condition on a variable that is declared in the body.
    } while (captureAndReturn { i + 1 } < 3)

    // At the end suppliers[0] = () -> 1, suppliers[1] = () -> 2, suppliers[2] = () -> 3 and
    // suppliers[3] = () -> 4
    assertEquals(4, suppliers.size)
    // TODO(b/468336770): Remove the condition `isJvm()` once the bug is fixed and the lambda
    // capture behavior matches Kotlin/JVM.
    if (isJvm()) {
      assertEquals(1, suppliers[0].get())
      assertEquals(2, suppliers[1].get())
      assertEquals(3, suppliers[2].get())
    }
    assertEquals(4, suppliers[3].get())
  }

  internal fun testLambdaCaptureFieldAndLocal() {
    val x = 1
    val result =
      test(
        IntToIntFunction { i: Int ->
          val y = 1
          x + y + field + i + 1
        },
        10,
      )
    assertTrue(result == 213)
  }
}

// This test below is not replicable in Kotlin. Kotlinc enforces that fun interface has exactly
// one abstract method.
// fun interface Equals<T> {
//   override fun equals(o: Any?): Boolean
//   fun get(): T? {
//     return null
//   }
// }
//
// interface SubEquals: Equals {
//   fun get(): String
// }
//
// private fun testSpecialLambdas() {
//   val getHello = SubEquals { "Hello" }
//
//   assertTrue(getHello == getHello)
//   assertNotEquals("Hello", getHello)
//   assertEquals("Hello", getHello.get())
// }

private fun testSpecializedLambda() {
  val stringConsumer = Consumer { s: java.lang.String ->
    var unused = s.substring(1)
  }
  val rawConsumer = stringConsumer as Consumer<Any>
  assertThrowsClassCastException({ rawConsumer.accept(Any()) }, "java.lang.String")

  val firstA: VarargsIntFunction<java.lang.String> =
    VarargsIntFunction<java.lang.String> { ns: Array<out java.lang.String> -> ns[0].indexOf("a") }
  val rawVarargsFunction = firstA as VarargsIntFunction<Any?>

  assertThrowsClassCastException(
    { rawVarargsFunction.apply(*arrayOf<Any>("bbabb", "aabb")) },
    "[Ljava.lang.String;",
  )
}

fun interface Consumer<T> {
  fun accept(t: T)
}

fun interface VarargsIntFunction<T> {
  fun apply(vararg t: T): Int
}

// Inside a function, a vararg-parameter of type T is visible as an array of T
// ss has the type Array<out T>, and it is covariant,
// it could refer to an array of String, or of any subtype of String.
// So there’s no value that’s safe to write to it
// trying to write to it will get error "inferred type is String but Nothing was expected"
private fun testVarargsLambdas() {
  val changeFirstElement =
    VarargsFunction<String> { ss: Array<out String> ->
      val returnArray = ss as Array<String>
      returnArray[0] = returnArray[0] + " world"
      returnArray
    }

  val params = arrayOf("hello")
  val returnedArray = changeFirstElement.apply(*params)
  // by using the spread operator, a copy of the initial array is passed to the lambda
  assertNotEquals(params, returnedArray)
  assertEquals("hello", params[0])
  assertEquals("hello world", returnedArray[0])
}

// Inside a function, a vararg-parameter of type T is visible as an array of T,
// t has the type Array<out T>
fun interface VarargsFunction<T> {
  fun apply(vararg t: T): Array<T>
}

// kotlin does not use var in lambda arguments
// private fun testVarKeywordInLambda() {
//   val f = IntToIntFunction { (var i: Int) -> i + 1 }
//   assertEquals(3, f.apply(2))
// }

// Kotlin lambda are automatically Serializable but koltin.io.Serializable is restricted
// private fun testSerializableLambda() {
//   Object lambda = (Consumer<Object>) o -> {};
//   assertTrue(lambda is Serializable)
// }

private fun testArbitraryNesting() {
  class A {
    fun a() {
      val x = intArrayOf(42)

      class B {
        fun b(): Int {
          val i: IntToIntFunction =
            object : IntToIntFunction {
              override fun apply(a: Int): Int {
                val ii: IntToIntFunction = IntToIntFunction { n: Int ->
                  object : IntToIntFunction {

                      override fun apply(b: Int): Int {
                        val iii = IntToIntFunction { m: Int ->
                          x[0] = x[0] + a + b + n + m
                          x[0]
                        }
                        return iii.apply(100)
                      }
                    }
                    .apply(200)
                }
                return ii.apply(300)
              }
            }
          return i.apply(400)
        }
      }

      val result = B().b()
      assertTrue(result == 1042)
      assertTrue(x[0] == 1042)
    }
  }
}

private fun testNestedLambdas() {
  val a = 10
  val i = IntToIntFunction { m: Int ->
    val b = 20
    val ii = IntToIntFunction { n: Int -> a + b + m + n }
    ii.apply(100)
  }
  assertTrue(i.apply(200) == 330)
}

private fun interface IdentityWithDefault<T> {
  fun identityaccept(t: T): T

  fun self(): IdentityWithDefault<T>? {
    return this
  }
}

const val MY_TEXT = "from Non Functional"

private interface InterfaceWithDefaultMethod {
  fun defaultMethod(): String? {
    return MY_TEXT
  }
}

// Kotlin does not support intersection types
// private fun testIntersectionTypeLambdas() {
//   val obj: Any =
//     IdentityWithDefault<String> { o: String? -> o } as IdentityWithDefault<String?>
//   assertTrue(obj is IdentityWithDefault<*>)
//   assertTrue(obj is InterfaceWithDefaultMethod)
//   assertEquals(obj, (obj as IdentityWithDefault<String?>).self())
//   assertEquals(
//     InterfaceWithDefaultMethod.MY_TEXT, (obj as InterfaceWithDefaultMethod).defaultMethod())
// }

private fun testInStaticContextWithInnerClasses() {
  val addTwo = IntToIntFunction { i: Int ->
    object : Any() {
        var storedValue = i

        fun addTwo(): Int = this.storedValue + 2
      }
      .addTwo()
  }
  assertEquals(5, addTwo.apply(3))
}
