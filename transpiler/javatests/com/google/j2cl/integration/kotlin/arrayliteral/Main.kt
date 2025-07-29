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
package arrayliteral

import com.google.j2cl.integration.testing.Asserts.assertThrowsArrayStoreException
import com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException
import com.google.j2cl.integration.testing.Asserts.assertTrue

class Foo

fun main(vararg unused: String) {
  testOneDimensionalLiteral()
  testOneDimensionalLiteral_empty()
  testOneDimensionalLiteral_terse()
  testOneDimensionalLiteral_subclassInInitializer()
  testOneDimensionalLiteral_withInitializer()
  testOneDimensionalLiteral_withInitializerThatEscapes()
  testTwoDimensionalLiteral()
  testTwoDimensionalLiteral_partial()
  testTwoDimensionalLiteral_unbalanced()
  testTwoDimensionalLiteral_implicitInstantiation()
}

private fun testOneDimensionalLiteral() {
  val oneD = intArrayOf(0, 1, 2)
  assertTrue(oneD.size == 3)

  // Values read fine.
  assertTrue(oneD[0] == 0)
  assertTrue(oneD[1] == 1)
  assertTrue(oneD[2] == 2)

  // Insertion to legal indexes is fine.
  oneD[0] = 5
  oneD[1] = 5
  oneD[2] = 5
}

private fun testOneDimensionalLiteral_empty() {
  val empty = arrayOf<Any>()
  assertTrue(empty.size == 0)
}

private fun testOneDimensionalLiteral_terse() {
  val terseLiteral = intArrayOf(0, 1, 2)
  assertTrue(terseLiteral.size == 3)
  assertTrue(terseLiteral[0] == 0)
  assertTrue(terseLiteral[1] == 1)
  assertTrue(terseLiteral[2] == 2)
}

private fun testOneDimensionalLiteral_subclassInInitializer() {
  val arrayContainer = arrayOf<Array<out Any?>>(arrayOfNulls<String>(1))
  assertTrue(arrayContainer[0]::class.java === Array<String>::class.java)
  assertThrowsArrayStoreException { (arrayContainer[0] as Array<Any?>)[0] = Any() }
}

fun createArrayThatEscapes(): Int {
  val array =
    IntArray(1) {
      return@createArrayThatEscapes 100
    }
  return 0
}

private fun doubleIt(x: Int): Int = x * 2

private fun doubleItNullable(x: Int?): Int = x!! * 2

private fun takesVarargs(vararg x: Int): Int = x[0] * 2

private fun hasTrailingVarargs(x: Int, vararg rest: Int): Int = x * 2

private fun testOneDimensionalLiteral_withInitializer() {
  var oneD = IntArray(3) { it * 2 }
  assertTrue(oneD.size == 3)

  // Values read fine.
  assertTrue(oneD[0] == 0)
  assertTrue(oneD[1] == 2)
  assertTrue(oneD[2] == 4)

  oneD = IntArray(3) { doubleIt(it) }
  assertTrue(oneD.size == 3)

  // Values read fine.
  assertTrue(oneD[0] == 0)
  assertTrue(oneD[1] == 2)
  assertTrue(oneD[2] == 4)

  oneD = IntArray(3) { x: Int? -> doubleItNullable(x) }
  assertTrue(oneD.size == 3)

  // Values read fine.
  assertTrue(oneD[0] == 0)
  assertTrue(oneD[1] == 2)
  assertTrue(oneD[2] == 4)

  oneD = IntArray(3, ::doubleIt)
  assertTrue(oneD.size == 3)

  // Values read fine.
  assertTrue(oneD[0] == 0)
  assertTrue(oneD[1] == 2)
  assertTrue(oneD[2] == 4)

  oneD = IntArray(3, ::doubleItNullable)
  assertTrue(oneD.size == 3)

  // Values read fine.
  assertTrue(oneD[0] == 0)
  assertTrue(oneD[1] == 2)
  assertTrue(oneD[2] == 4)

  val initializer = { x: Int -> x * 2 }
  oneD = IntArray(3, initializer)
  assertTrue(oneD.size == 3)

  // Values read fine.
  assertTrue(oneD[0] == 0)
  assertTrue(oneD[1] == 2)
  assertTrue(oneD[2] == 4)

  oneD = IntArray(3, ::takesVarargs)
  assertTrue(oneD.size == 3)

  // Values read fine.
  assertTrue(oneD[0] == 0)
  assertTrue(oneD[1] == 2)
  assertTrue(oneD[2] == 4)

  oneD = IntArray(3, ::hasTrailingVarargs)
  assertTrue(oneD.size == 3)

  // Values read fine.
  assertTrue(oneD[0] == 0)
  assertTrue(oneD[1] == 2)
  assertTrue(oneD[2] == 4)
}

private fun testOneDimensionalLiteral_withInitializerThatEscapes() {
  assertTrue(createArrayThatEscapes() == 100)
}

private fun testTwoDimensionalLiteral() {
  val foo = Foo()
  val twoD: Array<Array<Any>> = (arrayOf(arrayOf(foo, foo), arrayOf(foo, foo))) as Array<Array<Any>>
  assertTrue(twoD.size == 2)
  assertTrue(twoD[0].size == 2)

  // Values read fine.
  assertTrue(twoD[0][0] === foo)
  assertTrue(twoD[0][1] === foo)
  assertTrue(twoD[1][0] === foo)
  assertTrue(twoD[1][1] === foo)

  // Insertion to legal indexes is fine.
  twoD[0][0] = foo
  twoD[0][1] = foo
  twoD[1][0] = foo
  twoD[1][1] = foo

  // When inserting a leaf value the type must conform.
  assertThrowsArrayStoreException { twoD[0][0] = Any() }

  // The object-literal partial arrays still know their depth and so for example will reject an
  // attempt to stick an object into a 1-dimensional array slot.
  // Hide the cast of the array in a method call to avoid KotlinC to try to optimize item insertion
  // operation that leads to an inconsistent semantic.
  // See https://youtrack.jetbrains.com/issue/KT-55005
  assertThrowsArrayStoreException { castToArrayOfAny(twoD)[1] = Any() }
}

private fun castToArrayOfAny(arr: Any): Array<Any> = arr as Array<Any>

private fun testTwoDimensionalLiteral_partial() {
  val partial2D = arrayOf<Array<Any>?>(null)
  assertTrue(partial2D.size == 1)

  // Values read fine.
  assertTrue(partial2D[0] == null)

  // When trying to insert into the uninitialized section you'll get an NPE.
  assertThrowsNullPointerException { partial2D[0]!![0] = Any() }

  // You can replace it with a fully initialized array with the right dimensions.
  val full2D = arrayOf(arrayOfNulls<Any>(2), arrayOfNulls<Any>(2))
  assertTrue(full2D.size == 2)
  assertTrue(full2D[0].size == 2)
}

private fun testTwoDimensionalLiteral_unbalanced() {
  val unbalanced2D = arrayOf(arrayOf<Any?>(null, null), null)
  assertTrue(unbalanced2D.size == 2)

  // The first branch is actually fully initialized.
  assertTrue(unbalanced2D[0]!![0] == null)
  assertTrue(unbalanced2D[0]!!.size == 2)
  // The second branch less so.
  assertTrue(unbalanced2D[1] == null)
}

private fun testTwoDimensionalLiteral_implicitInstantiation() {
  val arrayLiteral2D = arrayOf(intArrayOf(1, 2), null)
  assertTrue(arrayLiteral2D.size == 2)
  assertTrue(arrayLiteral2D[0]!![0] == 1)
  assertTrue(arrayLiteral2D[0]!!.size == 2)
  assertTrue(arrayLiteral2D[1] == null)
}
