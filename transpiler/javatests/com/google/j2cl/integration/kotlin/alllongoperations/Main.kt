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
package alllongoperations

import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testInfixOperations()
  testPrefixOperations()
  testPostfixOperations()
  testAssignmentOperators()
  testOverAndUnderflow()
  testLiteralFormats()
  testExtendedOperands()
  testInitialization()
}

private fun testAssignmentOperators() {
  var a = 0L

  a += 1L
  assertTrue(a == 1L)

  a -= 2L
  assertTrue(a == -1L)

  a *= 30L
  assertTrue(a == -30L)

  a /= -10L
  assertTrue(a == 3L)

  a = a and 2L
  assertTrue(a == 2L)

  a = a or 4L
  assertTrue(a == 6L)

  a = a xor 3L
  assertTrue(a == 5L)

  a %= 2L
  assertTrue(a == 1L)

  a = a shl 3
  assertTrue(a == 8L)

  a = a shr 1
  assertTrue(a == 4L)

  a = a ushr 1
  assertTrue(a == 2L)
}

private fun testExtendedOperands() {
  val a = 1234L

  assertTrue(1254L == a + 5L + 5L + 5L + 5L)
}

private fun testInfixOperations() {
  val a = 1234L
  val b = 9876L
  val c = -55L

  assertTrue(12186984L == (a * b))
  assertTrue(8L == (b / a))
  assertTrue(4L == (b % a))
  assertTrue(11110L == (a + b))
  assertTrue(-8642L == (a - b))
  assertTrue(9872L == (a shl 3))
  assertTrue(-7L == (c shr 3))
  assertTrue(2305843009213693945L == (c ushr 3))
  assertTrue(a < b)
  assertTrue(b > a)
  assertTrue(b <= b)
  assertTrue(a <= b)
  assertTrue(b >= b)
  assertTrue(b >= a)
  assertTrue(1234L == a)
  assertTrue(a != b)
  assertTrue(8774L == (a xor b))
  assertTrue(1168L == (a and b))
  assertTrue(9942L == (a or b))
}

private fun testLiteralFormats() {
  // Small longs get a fromInt() fast path.
  // ************************************************************
  val a = 0x0011eeffL
  assertTrue(1175295L == a) // Hex

  // Kotlin has no octal literals.
  // val b = 01234567L
  // assertTrue(342391L == b) // Octal

  val c = 0b00011110L
  assertTrue(30L == c) // Binary

  // Huge longs fall back on fromString() construction.
  // ************************************************************
  val e = 0x20000000000001L
  assertTrue(9007199254740993L == e) // Hex

  // Kotlin has no octal literals.
  // val f = 0400000000000000001L
  // assertTrue(9007199254740993L == f) // Octal

  val g = 0b100000000000000000000000000000000000000000000000000001L
  assertTrue(9007199254740993L == g) // Binary
}

private fun testOverAndUnderflow() {
  val a = 999999999999999999L * 1000L
  assertTrue(3875820019684211736L == a)

  val b = 8999999999999999999L + 8999999999999999999L
  assertTrue(-446744073709551618L == b)

  val c = 999999999999999999L * -1000L
  assertTrue(-3875820019684211736L == c)

  val d = -8999999999999999999L - 8999999999999999999L
  assertTrue(446744073709551618L == d)
}

private fun testPostfixOperations() {
  var a = 100L

  assertTrue(100L == a++)
  assertTrue(101L == a--)
}

private fun testPrefixOperations() {
  var a = 100L

  assertTrue(101L == ++a)
  assertTrue(100L == --a)
  assertTrue(100L == +a)
  assertTrue(-100L == -a)
  assertTrue(-101L == a.inv())
}

private var fieldLong = 100L

private fun getReturnInitializerLong(): Long {
  return 100L
}

private fun testInitialization() {
  val localVariableLong = 100L

  assertTrue(localVariableLong == fieldLong)
  assertTrue(fieldLong == getReturnInitializerLong())
}
