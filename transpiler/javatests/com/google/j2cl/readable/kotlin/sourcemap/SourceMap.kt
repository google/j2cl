/*
 * Copyright 2024 Google Inc.
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
package sourcemap

import java.util.function.Function
import jsinterop.annotations.JsConstructor

// TODO(b/325660274): Improve the test coverage for sourcemap.
abstract class SourceMap<T : Number> @JsConstructor constructor(i: Int) : Comparator<T> {

  private var uninitializedInstanceField: Int

  // Instance initializer block
  init {
    uninitializedInstanceField = 1000
  }

  private var uninitializedInstanceField2: String

  // Another instance initializer block
  init {
    if (uninitializedInstanceField == 1000) {
      uninitializedInstanceField2 = "Hello!"
    } else if (uninitializedInstanceField == 2000) {
      uninitializedInstanceField2 = "World!"
    } else {
      uninitializedInstanceField2 = "Universe!"
    }
  }

  private constructor(
    uninitializedInstanceField: Int,
    uninitializedInstanceField2: String,
  ) : this(2)

  private var initializedInstanceField = 2

  companion object {
    internal var uninitializedStaticField: Double

    // Initializer in a static block
    init {
      uninitializedStaticField = 10.0
    }

    internal var initializedStaticField = "Hello"

    init {
      if (uninitializedStaticField == 10.0) {
        initializedStaticField = "World"
      }
    }
  }

  private fun testStatements(a: Int, b: Int, times: Int, number: Int): Int {
    var value = 0
    for (i in 0 until times) {
      value++
    }

    if (number % 2 == 0) {
      value += 1
    } else {
      value += 2
    }

    var b2 = b
    while (b2 > 0 && b2 < 100) {
      b2 -= 10
    }

    value += a + b2

    outerLoop@ for (i in 0..2) {
      when (number) {
        1 -> {
          value += 5
          break
        }
        2 -> {
          value += 2
          break@outerLoop
        }
        3 -> break@outerLoop
      }
      if (i == 1) break
    }

    return value
  }

  private fun testLambdaAndMethodReference(n: Int) {
    val f = { i: Int -> i + 1 }
    val f2 = ArrayList<Any>()::size
    val f3 = this::simpleMethod
  }

  private fun simpleMethod() = 1

  private fun testLocalClass() {
    class LocalClass
    LocalClass()
  }

  enum class Enum1() {
    VALUE1,
    VALUE2(),
    VALUE3(1);

    constructor(i: Int) : this()
  }

  private fun testWhenExpression() {
    var v = 1
    val r =
      when (v) {
        1,
        2 -> 10
        else -> 20
      }
  }

  private fun testInstanceOf(o: Any): Boolean {
    return o is String
  }

  private fun testFunctionExpression(): Function<Any?, Any?> {
    return Function { o: Any? -> o }
  }

  private fun testTypeLiteral(): Class<*> {
    return String::class.java
  }
}
