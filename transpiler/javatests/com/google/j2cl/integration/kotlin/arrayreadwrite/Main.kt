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
package arrayreadwrite

import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testObjects()
  testInts()
  testLongs()
  testMains()
  testCompoundArrayOperations()
}

private fun testLongs() {
  val longs = LongArray(100)
  var i = 0

  // Initial value.
  assertTrue(longs[0] == 0L)

  // Assignment.
  longs[0] = 10
  // Assignment is not an expression in Koltin
  // assertTrue((longs[0] = 10) == 10);
  assertTrue(longs[0] == 10L)

  // Compound assignment.
  longs[0] += 100L
  // Assignment is not an expression in Koltin
  // assertTrue((longs[0] += 100) == 110);
  assertTrue(longs[0] == 110L)

  // Preservation of side effects for compound assignment expressions
  // (Any expressions with side effects should be evaluated exactly once).
  i = 1
  // Assignment is not an expression in Koltin
  // assertTrue((longs[i++] += 1) == 1);
  longs[i++] += 1L
  assertTrue(i == 2)
  assertTrue(longs[0] == 110L)
  assertTrue(longs[1] == 1L) // 0 + 1 = 1
  assertTrue(longs[2] == 0L)

  longs[i++] -= 1L
  // Assignment is not an expression in Koltin
  // assertTrue((longs[i++] -= 1) == -1);
  assertTrue(i == 3)
  assertTrue(longs[1] == 1L)
  assertTrue(longs[2] == -1L) // 0 - 1 = -1
  assertTrue(longs[3] == 0L)

  longs[i] = 2
  longs[i++] *= 2L
  // Assignment is not an expression in Koltin
  //  assertTrue((longs[i++] *= 2) == 4);
  assertTrue(i == 4)
  assertTrue(longs[2] == -1L)
  assertTrue(longs[3] == 4L) // 2 * 2 = 4
  assertTrue(longs[4] == 0L)

  longs[i] = 10
  longs[i++] /= 2L
  // Assignment is not an expression in Koltin
  // assertTrue((longs[i++] /= 2) == 5);
  assertTrue(i == 5)
  assertTrue(longs[3] == 4L)
  assertTrue(longs[4] == 5L) // 10 / 2 = 5
  assertTrue(longs[5] == 0L)

  longs[i] = 10
  longs[i++] %= 4L
  // Assignment is not an expression in Koltin
  // assertTrue((longs[i++] %= 4) == 2);
  assertTrue(i == 6)
  assertTrue(longs[4] == 5L)
  assertTrue(longs[5] == 2L) // 10 % 4 = 2
  assertTrue(longs[6] == 0L)

  longs[i] = 7
  // bitwise and is an infix function in kotlin
  // longs[i++] &= 14
  longs[i] = longs[i++] and 14L
  // Assignment is not an expression in Koltin
  // assertTrue((longs[i++] &= 14) == 6);
  assertTrue(i == 7)
  assertTrue(longs[5] == 2L)
  assertTrue(longs[6] == 6L) // 7 & 14 = 6
  assertTrue(longs[7] == 0L)

  longs[i] = 8
  // bitwise and is an infix function in kotlin
  // longs[i++] |= 2
  longs[i] = longs[i++] or 2L
  // Assignment is not an expression in Koltin
  // assertTrue((longs[i++] |= 2) == 10);
  assertTrue(i == 8)
  assertTrue(longs[6] == 6L)
  assertTrue(longs[7] == 10L) // 8 | 2  = 10
  assertTrue(longs[8] == 0L)

  longs[i] = 7
  // bitwise xor is an infix function in kotlin
  // longs[i++] ^= 14
  longs[i] = longs[i++] xor 14L
  // Assignment is not an expression in Koltin
  // assertTrue((longs[i++] ^= 14) == 9);
  assertTrue(i == 9)
  assertTrue(longs[7] == 10L)
  assertTrue(longs[8] == 9L) // 7 ^ 14 = 9
  assertTrue(longs[9] == 0L)

  longs[i] = 2
  // bitwise shifts are infix functions in kotlin
  // longs[i++] <<= 1
  longs[i] = longs[i++] shl 1
  // Assignment is not an expression in Koltin
  // assertTrue((longs[i++] <<= 1) == 4);
  assertTrue(i == 10)
  assertTrue(longs[8] == 9L)
  assertTrue(longs[9] == 4L) // 2 << 1 = 4
  assertTrue(longs[10] == 0L)

  longs[i] = -10
  // bitwise shifts are infix functions in kotlin
  // longs[i++] >>= 2
  longs[i] = longs[i++] shr 2
  // Assignment is not an expression in Koltin
  // assertTrue((longs[i++] >>= 2) == -3);
  assertTrue(i == 11)
  assertTrue(longs[9] == 4L)
  assertTrue(longs[10] == -3L) // -10 >> 2 = -3
  assertTrue(longs[11] == 0L)

  longs[i] = -10
  // Assignment is not an expression in Koltin
  // assertTrue((longs[i++] >>>= 2) == 4611686018427387901L);
  longs[i] = longs[i++] ushr 2
  assertTrue(i == 12)
  assertTrue(longs[10] == -3L)
  assertTrue(longs[11] == 4611686018427387901L) // -10 >>> 2 = 4611686018427387901
  assertTrue(longs[12] == 0L)

  // Prefix and postfix expressions.
  i = 15
  assertTrue(++longs[i] == 1L)
  assertTrue(longs[15] == 1L)

  assertTrue(longs[i]++ == 1L)
  assertTrue(longs[15] == 2L)

  // Preservation of side effects for prefix and postfix expressions.
  assertTrue(++longs[++i] == 1L)
  assertTrue(i == 16)
  assertTrue(longs[15] == 2L)
  assertTrue(longs[16] == 1L)
  assertTrue(longs[17] == 0L)

  assertTrue(longs[++i]++ == 0L)
  assertTrue(i == 17)
  assertTrue(longs[16] == 1L)
  assertTrue(longs[17] == 1L)
  assertTrue(longs[18] == 0L)
}

private fun testInts() {
  val ints = IntArray(100)
  var i = 0

  // Initial value.
  assertTrue(ints[0] == 0)

  // Assignment.
  // Assignment is not an expression in Koltin
  // assertTrue((ints[0] = 10) == 10);
  ints[0] = 10
  assertTrue(ints[0] == 10)

  // Compound assignment.
  // Assignment is not an expression in Koltin
  // assertTrue((ints[0] += 100) == 110);
  ints[0] += 100
  assertTrue(ints[0] == 110)

  // Preservation of side effects in compound assignment
  // (Any expressions with side effects should be evaluated exactly once).
  i = 1
  // Assignment is not an expression in Koltin
  // assertTrue((ints[i++] += 1) == 1);
  ints[i++] += 1
  assertTrue(i == 2)
  assertTrue(ints[0] == 110)
  assertTrue(ints[1] == 1)
  assertTrue(ints[2] == 0)

  // Assignment is not an expression in Koltin
  // assertTrue((ints[i++] -= 1) == -1);
  ints[i++] -= 1
  assertTrue(i == 3)
  assertTrue(ints[1] == 1)
  assertTrue(ints[2] == -1) // 0 - 1 = -1
  assertTrue(ints[3] == 0)

  ints[i] = 2
  // Assignment is not an expression in Koltin
  // assertTrue((ints[i++] *= 2) == 4);
  ints[i++] *= 2
  assertTrue(i == 4)
  assertTrue(ints[2] == -1)
  assertTrue(ints[3] == 4) // 2 * 2 = 4
  assertTrue(ints[4] == 0)

  ints[i] = 10
  // Assignment is not an expression in Koltin
  // assertTrue((ints[i++] /= 2) == 5);
  ints[i++] /= 2
  assertTrue(i == 5)
  assertTrue(ints[3] == 4)
  assertTrue(ints[4] == 5) // 10 / 2 = 5
  assertTrue(ints[5] == 0)

  ints[i] = 10
  // Assignment is not an expression in Koltin
  // assertTrue((ints[i++] %= 4) == 2);
  ints[i++] %= 4
  assertTrue(i == 6)
  assertTrue(ints[4] == 5)
  assertTrue(ints[5] == 2) // 10 % 4 = 2
  assertTrue(ints[6] == 0)

  ints[i] = 7
  // Assignment is not an expression in Koltin
  // assertTrue((ints[i++] &= 14) == 6);
  ints[i] = ints[i++] and 14
  assertTrue(i == 7)
  assertTrue(ints[5] == 2)
  assertTrue(ints[6] == 6) // 7 & 14 = 6
  assertTrue(ints[7] == 0)

  ints[i] = 8
  // Assignment is not an expression in Koltin
  // assertTrue((ints[i++] |= 2) == 10);
  ints[i] = ints[i++] or 2
  assertTrue(i == 8)
  assertTrue(ints[6] == 6)
  assertTrue(ints[7] == 10) // 8 | 2  = 10
  assertTrue(ints[8] == 0)

  ints[i] = 7
  // Assignment is not an expression in Koltin
  //  assertTrue((ints[i++] ^= 14) == 9);
  ints[i] = ints[i++] xor 14
  assertTrue(i == 9)
  assertTrue(ints[7] == 10)
  assertTrue(ints[8] == 9) // 7 ^ 14 = 9
  assertTrue(ints[9] == 0)

  ints[i] = 2
  // Assignment is not an expression in Koltin
  // assertTrue((ints[i++] <<= 1) == 4);
  ints[i] = ints[i++] shl 1
  assertTrue(ints[8] == 9)
  assertTrue(ints[9] == 4) // 2 << 1 = 4
  assertTrue(ints[10] == 0)

  ints[i] = -10
  // Assignment is not an expression in Koltin
  // assertTrue((ints[i++] >>= 2) == -3);
  ints[i] = ints[i++] shr 2
  assertTrue(i == 11)
  assertTrue(ints[9] == 4)
  assertTrue(ints[10] == -3) // -10 >> 2 = -3
  assertTrue(ints[11] == 0)

  ints[i] = -10
  // Assignment is not an expression in Koltin
  // assertTrue((ints[i++] >>>= 2) == 1073741821);
  ints[i] = ints[i++] ushr 2
  assertTrue(i == 12)
  assertTrue(ints[10] == -3)
  assertTrue(ints[11] == 1073741821) // -10 >>> 2 = 1073741821
  assertTrue(ints[12] == 0)

  // Prefix and postfix expressions.
  i = 15
  assertTrue(++ints[i] == 1)
  assertTrue(ints[15] == 1)

  assertTrue(ints[i]++ == 1)
  assertTrue(ints[15] == 2)

  // Preservation of side effects for prefix and postfix expressions.
  assertTrue(++ints[++i] == 1)
  assertTrue(i == 16)
  assertTrue(ints[15] == 2)
  assertTrue(ints[16] == 1)
  assertTrue(ints[17] == 0)

  assertTrue(ints[++i]++ == 0)
  assertTrue(i == 17)
  assertTrue(ints[16] == 1)
  assertTrue(ints[17] == 1)
  assertTrue(ints[18] == 0)
}

private fun testObjects() {
  val expectedObject = Any()
  val objects = arrayOfNulls<Any>(100)

  // Initial value.
  assertTrue(objects[0] == null)
  assertTrue(objects[0] !== expectedObject)

  // Assignment.
  objects[0] = expectedObject
  assertTrue(objects[0] === expectedObject)

  // Array ctor with initializer
  val anArray = Array<String>(10) { index -> "$index" }
  assertTrue(anArray[0] == "0")
  assertTrue(anArray[9] == "9")
}

private fun testCompoundArrayOperations() {
  val intArray = IntArray(1)
  intArray[0] += 3
  assertTrue(intArray[0] == 3)

  intArray[0] /= 2
  assertTrue(intArray[0] == 1)

  val booleanArray = BooleanArray(1)
  assertFalse(booleanArray[0])
  booleanArray[0] = booleanArray[0] or true
  assertTrue(booleanArray[0])

  val longArray = LongArray(1)
  longArray[0] += 1L
  assertTrue(longArray[0] == 1L)

  val stringArray = arrayOf("first")
  stringArray[0] += "second"
  assertTrue(stringArray[0] == "firstsecond")
}

private fun testMains() {
  val m = Main()
  val mains = arrayOfNulls<Main>(100)

  mains[0] = null
  assertTrue(mains[0] == null)

  mains[0] = m
  assertTrue(mains[0] === m)
}

class Main

class Foo(val f: Int)

private fun testArrayOfFunctions() {
  val objects = arrayOf(Foo(0), Foo(1))
  assertTrue(objects[0].f == 0)
  assertTrue(objects[1].f == 1)

  val ints = intArrayOf(1, 2)
  assertTrue(ints[0] == 1)
  assertTrue(ints[1] == 2)

  val booleans = booleanArrayOf(true, false)
  assertTrue(booleans[0])
  assertFalse(booleans[1])

  val chars = charArrayOf('a', 'b')
  assertTrue(chars[0] == 'a')
  assertTrue(chars[1] == 'b')

  val shorts = shortArrayOf(1, 2)
  assertTrue(shorts[0] == 1.toShort())
  assertTrue(shorts[1] == 2.toShort())

  val longs = longArrayOf(1, 2)
  assertTrue(longs[0] == 1L)
  assertTrue(longs[1] == 2L)

  val doubles = doubleArrayOf(1.0, 2.0)
  assertTrue(doubles[0] == 1.0)
  assertTrue(doubles[1] == 2.0)

  val floats = floatArrayOf(1f, 2f)
  assertTrue(floats[0] == 1f)
  assertTrue(floats[1] == 2f)
}
