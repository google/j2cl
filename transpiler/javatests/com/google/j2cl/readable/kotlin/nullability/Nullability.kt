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
package nullability

import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsOptional

class Nullability @JsConstructor constructor(a: String) {
  private val f1: String = "Hello"
  private val f2: String? = null

  private val f4: List<String> = ArrayList()
  private var f5: List<String>? = ArrayList()
  private val f6: List<String?> = ArrayList()
  private val f7: List<String?>? = null

  private val f8: Array<String> = arrayOf()
  // Nonnullable array of nullable strings.
  private val f9: Array<String?> = arrayOf()
  // Nullable array of non-nullable strings.
  private val f10: Array<String>? = arrayOf()
  // Conversion from generic type parameter.
  private val f12: List<String?>? = ArrayList()

  private val f13: Any
  private val f14: Any? = null

  init {
    f13 = Any()
  }

  fun m1(a: String, b: List<Double>, c: String?): String {
    return ""
  }

  fun m2(a: String?, b: List<Double?>): String? {
    return ""
  }

  fun m3(a: String?, vararg args: String): String? {
    return null
  }

  fun m4(f: MyFunction) {}

  @JsMethod fun m5(a: String, @JsOptional jsOptional: String?) {}

  fun testPrimitive() {
    val a: Boolean = true
    val b: Boolean? = true
    val c: Byte = 0
    val d: Byte? = null
    val e: Char = 'a'
    val f: Char? = null
    val g: Double = 0.0
    val h: Double? = null
    val i: Float = 0.0f
    val j: Float? = null
    val k: Int = 0
    val l: Int? = null
    val m: Long = 0
    val n: Long? = null
    val o: Short = 0
    val p: Short? = null
    val q: Unit? = null
  }
}

class Foo<T> {
  fun bar(t: T) {}

  fun baz(): T? {
    return null
  }

  @JsMethod fun foo(a: String, @JsOptional jsOptional: T?) {}

  override fun toString(): String {
    return "Foo"
  }
}

interface MyFunction {
  fun x(a: String): String
}

interface StringList : List<String?>

// Should implement Comparator<string>
class StringComparator : Comparator<String> {
  override fun compare(a: String, b: String): Int {
    return 0
  }
}

// Should implement Comparator<?string>
class NullableStringComparator : Comparator<String?> {
  override fun compare(a: String?, b: String?): Int {
    return 0
  }
}

internal interface NullableTemplatedReturn<T> {
  fun foo(): T?
}

class NullableTemplatedReturnOverride : NullableTemplatedReturn<String?> {
  override fun foo(): String? {
    return "foo"
  }
}

internal class ParameterizedNonNullable<N : Any> {
  fun getNullable(): N? = null

  fun getDefaultNullability(): N = throw RuntimeException()
}

internal class ParameterizedNullable<N> {
  fun getNullable(): N? = null

  fun getDefaultNullability(): N = throw RuntimeException()
}

internal class NonNullableInsideNullable<T> {
  fun nonNullableTest(nonNullable: ParameterizedNonNullable<T & Any>) {}
}

interface Marker

open class Recursive<T> where T : Recursive<T>, T : Marker?

open class RecursiveNullable<T> where T : RecursiveNullable<T>?, T : Marker?

class RecursiveChild : Recursive<RecursiveChild>(), Marker

class RecursiveNullableChild : RecursiveNullable<RecursiveNullableChild?>(), Marker

class RecursiveParam<T> where T : Recursive<T>, T : Marker?

class RecursiveNullableParam<T> where T : RecursiveNullable<T>?, T : Marker?

fun <T> testRecursive() where T : Recursive<T>, T : Marker? {
  val generic = RecursiveParam<T>()
  val parametrized = RecursiveParam<RecursiveChild>()
}

fun <T> testRecursiveNullable() where T : RecursiveNullable<T>?, T : Marker? {
  val generic = RecursiveNullableParam<T>()
  val parametrized = RecursiveNullableParam<RecursiveNullableChild?>()
}
