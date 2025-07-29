/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package foreachstatement

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue
import java.io.Serializable

/** Test for for loops. */
fun main(vararg unused: String) {
  testForEachArray()
  testForEachArray_nested()
  testForEachArray_boxed()
  testForEachArray_unboxed()
  testForEachIterable()
  testForEachIterable_typeVariable<Any, Iterable<Any>>()
  testForEachIterable_intersection<MyIterable, MyIterable>()
  // Tests that don't have a way to be expressed in Kotlin.
  // testForEachIterable_union();
  // testForEachIterable_widening();
}

internal open class MyIterable : Iterable<Int>, Serializable {
  internal inner class MyIterator : Iterator<Int> {
    private var i = 5

    override fun hasNext(): Boolean {
      return i >= 1
    }

    override fun next(): Int {
      return i--
    }
  }

  override fun iterator(): MyIterator {
    return MyIterator()
  }
}

private fun testForEachArray() {
  val array = intArrayOf(5, 4, 3, 2, 1)
  var j = 5
  var lastSeenInt = -1
  for (i in array) {
    assertTrue("Seen:<$i> Expected:<$j>", i == j)
    j--
    lastSeenInt = i
  }
  assertTrue("LastSeen:<$lastSeenInt> should be one", lastSeenInt == 1)
}

private fun testForEachArray_nested() {
  val matrix = arrayOf(IntArray(5), IntArray(5), IntArray(5), IntArray(5), IntArray(5))
  // load the matrix
  for (i in 0..4) {
    for (k in 0..4) {
      matrix[i][k] = value(i, k)
    }
  }
  var i0 = 0
  for (row in matrix) {
    var k0 = 0
    for (k in row) {
      assertTrue("Seen:<" + k + "> Expected:<" + value(i0, k0) + ">", k == value(i0, k0))
      k0++
    }
    i0++
  }
}

private fun testForEachArray_boxed() {
  var concatName = ""
  for (i in intArrayOf(1, 2, 3)) {
    concatName += i.javaClass.simpleName
  }
  assertEquals("intintint", concatName)
}

private fun testForEachArray_unboxed() {
  var sum = 0
  for (i in arrayOf(1, 2, 3)) {
    sum += i
  }
  assertEquals(6, sum)
}

private fun value(i: Int, j: Int): Int {
  return i * 10 + j
}

private fun testForEachIterable() {
  var lastSeenInteger = -1
  var j = 5
  for (s in MyIterable()) {
    assertTrue("Seen:<$s> Expected:<$j>", s == j)
    j--
    lastSeenInteger = s
  }
  assertTrue("LastSeen:<$lastSeenInteger> should be one", lastSeenInteger == 1)
}

private fun <T, C : Iterable<T>> testForEachIterable_typeVariable() {
  val iterable =
    object : Iterable<T> {
      private val elements = arrayOf(1, 2, 3)

      override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
          var index = 0

          override fun hasNext(): Boolean {
            return index < elements.size
          }

          override fun next(): T {
            return elements[index++] as T
          }
        }
      }
    }
      as C
  var onetwothree = ""
  for (e in iterable) {
    onetwothree += e
  }
  assertEquals("123", onetwothree)
}

private fun <T, S : T> testForEachIterable_intersection() where T : MyIterable, T : Serializable {
  val iterable = MyIterable() as S
  var lastSeenInteger = -1
  var j = 5
  for (s in iterable) {
    assertTrue("Seen:<$s> Expected:<$j>", s == j)
    j--
    lastSeenInteger = s
  }
  assertTrue("LastSeen:<$lastSeenInteger> should be zero", lastSeenInteger == 1)
}
