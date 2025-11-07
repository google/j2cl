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
package varargs

import java.io.Serializable
import java.lang.Cloneable

open class Varargs(vararg args: Int) {
  fun interface FunctionalInterface {
    fun apply(vararg strings: String)
  }

  private var args: Array<out Any?> = arrayOfNulls(0)

  constructor() : this(1) {}

  fun test(a: Int, vararg args: Any?) {}

  fun testCloneable(vararg args: Cloneable?) {}

  fun testSerializable(vararg args: Serializable?) {}

  fun testAssignment(vararg args: Any) {
    this.args = args
  }

  fun testLambda(functionalInterface: FunctionalInterface) {}

  fun testNonTrailingVararg(a: Int, vararg args: Int, b: Int) {}

  fun testNonTrailingVarargWithDefault(a: Int, vararg args: Int, b: Int = 0) {}

  fun testOverloaded(o: Any?) {}

  fun testOverloaded(o: String?, vararg rest: Any?) {}

  fun testOverloaded(o: Long) {}

  fun testOverloaded(o: Long, vararg test: Long) {}

  fun main() {
    val v = Varargs()
    v.test(1)
    v.test(1, Any())
    v.test(1, *arrayOf(Any()))
    v.test(1, Any(), *arrayOf(Any()), Any())
    v.test(1, *arrayOf<Any>())
    v.test(1, *arrayOf<Array<Any>>())
    v.test(1, *listToArray(listOf(Any())))
    v.test(1, *passthrough(arrayOf("a")))
    // TODO(b/458612609): `kotlin.Cloneable` is not supported and Kotlin Arrays do not implement
    // `java.lang.Cloneable`.
    // v.testCloneable(*arrayOf<Array<Any>>())
    v.testSerializable(*arrayOf<Array<Any>>())
    v.testLambda { strings: Array<out String> -> args = strings }
    v.testNonTrailingVararg(1, 2, 3, b = 4)
    v.testNonTrailingVararg(1, *intArrayOf(2, 3), b = 4)

    v.testNonTrailingVarargWithDefault(1, 2, 3)
    v.testNonTrailingVarargWithDefault(1, *intArrayOf(2, 3))

    // According to Kotlin Specification §11.4.2 this should be calling
    // testOverloaded(String, Any...)
    v.testOverloaded("foo")
    // This will be calling testOverloaded(String, Any...)
    v.testOverloaded("foo", "bar")
    // This will be calling testOverloaded(Any)
    v.testOverloaded("" as Any)
    // This will be calling testOverloaded(Long)
    v.testOverloaded(1)
    // This will be calling testOverloaded(Any?)
    v.testOverloaded(1L as Long?)
    // This will be calling testOverloaded(Long)
    v.testOverloaded(1L)
    // This will be calling testOverloaded(Long, Long...)
    v.testOverloaded(1L, 2, 3L)
    // This will be calling testOverloaded(Long, Long...)
    v.testOverloaded(1, 2, 3L)
  }
}

fun <T : Number> f(vararg elements: T) {}

fun <E : Number> bar(a: E, b: E) {
  f(a, b)
}

internal class Child : Varargs(1)

inline fun <reified T> listToArray(list: List<T>) = Array<T>(list.size) { list[it] }

fun <T> passthrough(o: T) = o

private fun <T : Comparable<T>> indirectSpread(a: Array<T>) = generics(*a)

private fun <T> generics(vararg element: T) {}
