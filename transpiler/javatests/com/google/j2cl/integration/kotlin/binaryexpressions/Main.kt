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
package binaryexpressions

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertEqualsDelta
import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Test basic binary operations. This test does not aim to test primitive overflow and Coercion. */
fun main(vararg unused: String) {
  testArithmetic()
  testFloatingPointRemainder()
  testOverflow()
  testBooleanOperations()
  testStringConcatenation()
  testExtendedOperands()
  testFloatConsistency()
  testParenthesizedLvalues()
  testSideEffects()
}

private fun testArithmetic() {
  val a = 6
  val b = 3
  val c = -1
  assertTrue(a * b == 18)
  assertTrue(a / b == 2)
  assertTrue(a % b == 0)
  assertTrue(a + b == 9)
  assertTrue(a - b == 3)
  assertTrue(a shl 1 == 12)
  assertTrue(c shr 16 == -1)
  assertTrue(c ushr 16 == 65535)
  assertTrue(a > b)
  assertTrue(a >= b)
  assertTrue(b < a)
  assertTrue(b <= a)
  assertTrue(a != b)
  assertTrue((a xor b) == 5)
  assertTrue((a and b) == 2)
  assertTrue((a or b) == 7)
  assertTrue((a > b) && (a == 6))
  assertTrue((a < b) || (a == 6))
  assertTrue((-1 ushr 0) == -1)

  assertTrue(a + b + a - b == 12)
  assertTrue((a + b) * (a - b) == 27)

  assertTrue(((5 / 2) - 0.0) == 2.0)

  var i = 1
  i += 1L.toInt()
  assertTrue(i == 2)
  i = (i.toDouble() + Double.MAX_VALUE).toInt()
  assertTrue(i == Int.MAX_VALUE)

  var d = 10
  assertTrue(d == 10)
  d = d xor d
  assertTrue(d == 0)
  d += 15
  assertTrue(d == 15)
  d -= 5
  assertTrue(d == 10)
  d *= 2
  assertTrue(d == 20)
  d /= 4
  assertTrue(d == 5)
  d = d and 3
  assertTrue(d == 1)
  d = d or 2
  assertTrue(d == 3)
  d %= 2
  assertTrue(d == 1)
  d = d shl 3
  assertTrue(d == 8)
  d = d shr 3
  assertTrue(d == 1)
  d = -1
  d = d ushr 16
  assertTrue(d == 65535)
  d = -1
  d = d ushr 0
  assertTrue(d == -1)

  // Compound assignment with enclosing instance.
  class Outer {
    var e = 0

    inner class Inner {
      init {
        e += 1
      }
    }
  }

  val finalOuter = Outer()
  finalOuter.e += 1
  assertTrue(finalOuter.e == 1)

  var outer: Outer? = Outer()
  val copy = outer!!
  outer.e +=
    if (true) {
      outer = null
      1
    } else 0
  assertTrue(copy.e == 1)

  val outer2 = Outer()
  outer2.Inner()
  assertTrue(outer2.e == 1)

  // Make sure that promotion rules from shift operations are correct.
  assertTrue(Int.MAX_VALUE shl 1L.toInt() == -2)
}

private fun testFloatingPointRemainder() {
  assertTrue((Double.NaN % 1.2).isNaN())
  assertTrue((1.2 % Double.NaN).isNaN())
  assertTrue((-1.2 % Double.NaN).isNaN())
  assertTrue((Double.POSITIVE_INFINITY % 1.2).isNaN())
  assertTrue((Double.NEGATIVE_INFINITY % 1.2).isNaN())
  assertTrue((-1.2 % 0.0).isNaN())
  assertTrue(1.2 % Double.POSITIVE_INFINITY == 1.2)
  assertTrue(1.2 % Double.NEGATIVE_INFINITY == 1.2)
  assertTrue(Long.MAX_VALUE % 3.0 == 2.0)
  assertTrue(Long.MAX_VALUE % -3.0 == 2.0)
  assertTrue(Long.MIN_VALUE % 3.0 == -2.0)
  assertTrue(Long.MIN_VALUE % -3.0 == -2.0)
  assertEquals(1.0 % 1.0, 0.0)
  assertEquals(-1.0 % 1.0, -0.0)
  assertEquals(1.0 % -1.0, 0.0)
  assertEquals(-1.0 % -1.0, -0.0)
  assertEquals(0.0 % 1.2, 0.0)
  assertEquals(-0.0 % 1.2, -0.0)
  assertEqualsDelta(.2, .7 % .5, 1e-15)
  assertEqualsDelta(.2, .7 % -.5, 1e-15)
  assertEqualsDelta(-.2, -.7 % .5, 1e-15)
  assertEqualsDelta(-.2, -.7 % -.5, 1e-15)

  assertTrue((Float.NaN % 1.2f).isNaN())
  assertTrue((1.2f % Float.NaN).isNaN())
  assertTrue((-1.2f % Float.NaN).isNaN())
  assertTrue((Float.POSITIVE_INFINITY % 1.2f).isNaN())
  assertTrue((Float.NEGATIVE_INFINITY % 1.2f).isNaN())
  assertTrue((-1.2f % 0.0f).isNaN())
  assertTrue(1.2f % Float.POSITIVE_INFINITY == 1.2f)
  assertTrue(1.2f % Float.NEGATIVE_INFINITY == 1.2f)
  assertTrue(Long.MAX_VALUE % 3.0f == 2.0f)
  assertTrue(Long.MAX_VALUE % -3.0f == 2.0f)
  assertTrue(Long.MIN_VALUE % 3.0f == -2.0f)
  assertTrue(Long.MIN_VALUE % -3.0f == -2.0f)
  assertEquals(1.0f % 1.0f, 0.0f)
  assertEquals(-1.0f % 1.0f, -0.0f)
  assertEquals(1.0f % -1.0f, 0.0f)
  assertEquals(-1.0f % -1.0f, -0.0f)
  assertEquals(0.0f % 1.2f, 0.0f)
  assertEquals(-0.0f % 1.2f, -0.0f)
  assertEqualsDelta(.2f, .7f % .5f, 1e-7f)
  assertEqualsDelta(.2f, .7f % -.5f, 1e-7f)
  assertEqualsDelta(-.2f, -.7f % .5f, 1e-7f)
  assertEqualsDelta(-.2f, -.7f % -.5f, 1e-7f)
}

private fun testOverflow() {
  assertTrue(Byte.MAX_VALUE * Byte.MAX_VALUE == 0x3f01)
  assertTrue(Short.MAX_VALUE * Short.MAX_VALUE == 0x3fff0001)
  // Overflows.
  assertTrue(Long.MAX_VALUE * Long.MAX_VALUE == 1L)
  // times operator does not exist for Char
  // assertTrue(Char.MAX_VALUE * Char.MAX_VALUE == -131071 /*0xfffe0001*/)
  assertTrue(Int.MAX_VALUE * Int.MAX_VALUE == 1)

  assertTrue(Int.MAX_VALUE + Int.MAX_VALUE == -2)
  assertTrue((Int.MAX_VALUE + Int.MAX_VALUE).toDouble() == -2.0)
}

private fun testBooleanOperations() {
  var bool = true
  bool = bool and false
  assertTrue(!bool)
  bool = bool or true
  assertTrue(bool)
}

private fun testStringConcatenation() {
  val foo = "foo"
  val bar = "bar"
  assertEquals("foobar", foo + bar)

  val s: String? = null
  assertEquals("nullnull", s + s)

  val stringArray: Array<String?> = arrayOfNulls(1)

  assertEquals(stringArray[0] + stringArray[0], "nullnull")

  val bool = false
  assertEquals("false", "" + bool)
}

private fun testExtendedOperands() {
  // Binary expression from JDT InfixExpression with extended operands.
  val n = 1
  val l = 2L
  assertTrue(20 + l + n == n + l + 20)
}

private val FLOAT_CONSTANT = 1.1f
private val DOUBLE_CONSTANT = FLOAT_CONSTANT
private val SUM = FLOAT_CONSTANT + FLOAT_CONSTANT

private fun testFloatConsistency() {
  val floatSum = FLOAT_CONSTANT + FLOAT_CONSTANT
  assertTrue(floatSum == FLOAT_CONSTANT + FLOAT_CONSTANT)
  assertTrue(floatSum == SUM)
  assertTrue(DOUBLE_CONSTANT == FLOAT_CONSTANT)
}

private fun testParenthesizedLvalues() {
  var l = 1
  ((l))++
  assertTrue(l == 2)

  ((((l)))) += 2
  assertTrue(l == 4)
}

private var sideEffectCallCounter = 0

private fun getDoubleWithSideEffect(d: Double): Double {
  sideEffectCallCounter++
  return d
}

private fun testSideEffects() {
  var d = 3.0
  sideEffectCallCounter = 0
  d -= getDoubleWithSideEffect(2.0)
  assertTrue(d == 1.0)
  assertTrue(sideEffectCallCounter == 1)
}
