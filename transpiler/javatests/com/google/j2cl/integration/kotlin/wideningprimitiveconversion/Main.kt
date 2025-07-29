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
package wideningprimitiveconversion

import com.google.j2cl.integration.testing.Asserts.assertTrue

private fun testAssignment() {
  testArraysAssignment()
  testCompoundAssignment()
  testSimpleAssignment()
  testReturnAssignment()
  testFieldInitializerAssignment()
  testVariableInitializerAssignment()
  testTernaryAssignment()
  testStaticCoercions()
}

private class ClassTakesDouble(widenValue: Double, expectValue: Double) {
  init {
    assertTrue(widenValue == expectValue)
  }
}

private class ClassTakesFloat(widenValue: Float, expectValue: Float) {
  init {
    assertTrue(widenValue == expectValue)
  }
}

private class ClassTakesInt(widenValue: Int, expectValue: Int) {
  init {
    assertTrue(widenValue == expectValue)
  }
}

private class ClassTakesLong(widenValue: Long, expectValue: Long) {
  init {
    assertTrue(widenValue == expectValue)
  }
}

private const val fieldIntFromChar = 456.toChar().code
private const val fieldLongFromInt: Long = 456
private const val fieldFloatFromLong = 456f
private const val fieldDoubleFromLong = 456.0

fun main(vararg unused: String) {
  testAssignment()
  testBinaryNumericPromotion()
  testCast()
  testMethodInvocation()
}

private fun returnDoubleFromLong(): Double {
  return 123L.toDouble()
}

private fun returnFloatFromLong(): Float {
  return 123L.toFloat()
}

private fun returnIntFromChar(): Int {
  return 123.toChar().code
}

private fun returnLongFromInt(): Long {
  return 123.toLong()
}

private fun takesDouble(widenValue: Double, expectValue: Double) {
  assertTrue(widenValue == expectValue)
}

private fun takesFloat(widenValue: Float, expectValue: Float) {
  assertTrue(widenValue == expectValue)
}

private fun takesInt(widenValue: Int, expectValue: Int) {
  assertTrue(widenValue == expectValue)
}

private fun takesLong(widenValue: Long, expectValue: Long) {
  assertTrue(widenValue == expectValue)
}

private fun testArraysAssignment() {
  val b: Byte = 1
  val c = 'a'

  // Arrays
  val ints = IntArray(c.code)
  assertTrue(ints.size == 97)
  ints[b.toInt()] = b.toInt()
  assertTrue(ints[1] == 1)
  val doubles = doubleArrayOf(100L.toDouble(), 200L.toDouble(), 300L.toDouble())
  assertTrue(doubles[0] == 100.0)
}

private fun testBinaryNumericPromotion() {
  val b: Byte = 1
  val mb: Byte = 127 // Byte.MAX_VALUE
  val c = 'a'
  val mc = '\uFFFF' // Character.MAX_VALUE;
  val s: Short = 2
  val ms: Short = 32767 // Short.MAX_VALUE;
  val i = 3
  val mi = 2147483647 // Integer.MAX_VALUE;
  val l = 4L
  val ll = 2415919103L // max_int < ll < max_int * 2, used for testing for signs.
  val ml = 9223372036854775807L // Long.MAX_VALUE;
  val f = 2.7f
  val mf = 3.4028235E38f // Float.MAX_VALUE;
  val d = 2.6

  // Anything below int promotes to int.
  assertTrue(mb * mb == mb.toInt() * mb.toInt())
  assertTrue(mb * mc.code == mb.toInt() * mc.code)
  assertTrue(mb * ms == mb.toInt() * ms.toInt())
  assertTrue(mb * mi == mb.toInt() * mi)

  // If there is a long then anything below long promotes to long.
  assertTrue(l * mb == l * mb.toLong())
  assertTrue(l * mc.code == l * mc.code.toLong())
  assertTrue(l * ms == l * ms.toLong())
  assertTrue(l * mi == l * mi.toLong())

  // If there is a float then anything below float promotes to float.
  assertTrue(f * mb == f * mb.toFloat())
  assertTrue(f * mc.code == f * mc.code.toFloat())
  assertTrue(f * ms == f * ms.toFloat())
  assertTrue(f * mi == f * mi.toFloat())
  assertTrue(f * ml == f * ml.toFloat())

  // If there is a double then anything below double promotes to double.
  assertTrue(d * mb == d * mb.toDouble())
  assertTrue(d * mc.code == d * mc.code.toDouble())
  assertTrue(d * ms == d * ms.toDouble())
  assertTrue(d * mi == d * mi.toDouble())
  assertTrue(d * ml == d * ml.toDouble())
  assertTrue(d * mf == d * mf.toDouble())

  // And with equality operators.
  assertTrue(i.toLong() == l - 1L)
  assertTrue(i.toLong() != l)
  assertTrue(i < l)
  assertTrue(i <= l)
  assertTrue(i <= l)
  assertTrue(i < l)
}

private fun testCast() {
  val b: Byte = 1
  val mb: Byte = 127 // Byte.MAX_VALUE
  val c = 'a'
  val mc = '\uFFFF' // Character.MAX_VALUE;
  val s: Short = 2
  val ms: Short = 32767 // Short.MAX_VALUE;
  val i = 3
  val mi = 2147483647 // Integer.MAX_VALUE;
  val l = 4L
  val ll = 2415919103L // max_int < ll < max_int * 2, used for testing for signs.
  val ml = 9223372036854775807L // Long.MAX_VALUE;
  val f = 2.7f
  val mf = 3.4028235E38f // Float.MAX_VALUE;
  val d = 2.6

  assertTrue(b.toShort().toInt() == 1)
  assertTrue(b.toInt() == 1)
  assertTrue(b.toLong() == 1L)
  assertTrue(b.toFloat() == 1.0f)
  assertTrue(b.toDouble() == 1.0)

  assertTrue(mb.toShort().toInt() == 127)
  assertTrue(mb.toInt() == 127)
  assertTrue(mb.toLong() == 127L)
  assertTrue(mb.toFloat() == 127.0f)
  assertTrue(mb.toDouble() == 127.0)

  assertTrue(c.code == 97)
  assertTrue(c.code.toLong() == 97L)
  assertTrue(c.code.toFloat() == 97.0f)
  assertTrue(c.code.toDouble() == 97.0)

  assertTrue(mc.code == 65535)
  assertTrue(mc.code.toLong() == 65535L)
  assertTrue(mc.code.toFloat() == 65535.0f)
  assertTrue(mc.code.toDouble() == 65535.0)

  assertTrue(s.toInt() == 2)
  assertTrue(s.toLong() == 2L)
  assertTrue(s.toFloat() == 2.0f)
  assertTrue(s.toDouble() == 2.0)

  assertTrue(ms.toInt() == 32767)
  assertTrue(ms.toLong() == 32767L)
  assertTrue(ms.toFloat() == 32767.0f)
  assertTrue(ms.toDouble() == 32767.0)

  assertTrue(i.toLong() == 3L)
  assertTrue(i.toFloat() == 3.0f)
  assertTrue(i.toDouble() == 3.0)

  assertTrue(mi.toLong() == 2147483647L)
  assertTrue(mi.toDouble() == 2.147483647E9)

  assertTrue(l.toFloat() == 4f)
  assertTrue(l.toDouble() == 4.0)

  assertTrue(ll.toDouble() == 2.415919103E9)

  assertTrue(ml.toFloat() == 9.223372036854776E18f)
  assertTrue(ml.toDouble() == 9.223372036854776E18)

  assertTrue(f.toDouble() - 2.7 < 1e-7)

  assertTrue(mf.toDouble() == 3.4028234663852886e+38)
}

private fun testCompoundAssignment() {
  val i = 3
  val l = 6L

  var ri = 0
  var rl: Long = 0

  // Compound Assignment
  ri += l.toInt()
  assertTrue(ri == 6)
  rl += i.toLong()
  assertTrue(rl == 3L)
}

private fun testFieldInitializerAssignment() {
  assertTrue(fieldIntFromChar == 456)
  assertTrue(fieldLongFromInt == 456L)
  assertTrue(fieldFloatFromLong == 456f)
  assertTrue(fieldDoubleFromLong == 456.0)
}

private fun testMethodInvocation() {
  val c = '\u0064' // 100
  val i = 200
  val l = 300L

  takesInt(c.code, 100)
  takesLong(i.toLong(), 200L)
  takesFloat(l.toFloat(), 300f)
  takesDouble(l.toDouble(), 300.0)

  ClassTakesInt(c.code, 100)
  ClassTakesLong(i.toLong(), 200L)
  ClassTakesFloat(l.toFloat(), 300f)
  ClassTakesDouble(l.toDouble(), 300.0)
}

private fun testReturnAssignment() {
  assertTrue(returnIntFromChar() == 123)
  assertTrue(returnLongFromInt() == 123L)
  assertTrue(returnFloatFromLong() == 123f)
  assertTrue(returnDoubleFromLong() == 123.0)
}

private fun testSimpleAssignment() {
  val b: Byte = 1
  val mb: Byte = 127 // Byte.MAX_VALUE
  val c = 'a'
  val mc = '\uFFFF' // Character.MAX_VALUE;
  val s: Short = 2
  val ms: Short = 32767 // Short.MAX_VALUE;
  val i = 3
  val mi = 2147483647 // Integer.MAX_VALUE;
  val l = 4L
  val ll = 2415919103L // max_int < ll < max_int * 2, used for testing for signs.
  val ml = 9223372036854775807L // Long.MAX_VALUE;
  val f = 2.7f
  val mf = 3.4028235E38f // Float.MAX_VALUE;

  val rb: Byte = 0
  val rc = '\u0000'
  val rs: Short = 0
  var ri = 0
  var rl = 0L
  var rf = 0f
  var rd = 0.0

  // Exhaustive simple assignment
  assertTrue(b.toInt().also { ri = it } == 1)
  assertTrue(b.toLong().also { rl = it } == 1L)
  assertTrue(b.toFloat().also { rf = it } == 1.0f)
  assertTrue(b.toDouble().also { rd = it } == 1.0)

  assertTrue(mb.toInt().also { ri = it } == 127)
  assertTrue(mb.toLong().also { rl = it } == 127L)
  assertTrue(mb.toFloat().also { rf = it } == 127.0f)
  assertTrue(mb.toDouble().also { rd = it } == 127.0)

  assertTrue(c.code.also { ri = it } == 97)
  assertTrue(c.code.toLong().also { rl = it } == 97L)
  assertTrue(c.code.toFloat().also { rf = it } == 97.0f)
  assertTrue(c.code.toDouble().also { rd = it } == 97.0)

  assertTrue(mc.code.also { ri = it } == 65535)
  assertTrue(mc.code.toLong().also { rl = it } == 65535L)
  assertTrue(mc.code.toFloat().also { rf = it } == 65535.0f)
  assertTrue(mc.code.toDouble().also { rd = it } == 65535.0)

  assertTrue(s.toInt().also { ri = it } == 2)
  assertTrue(s.toLong().also { rl = it } == 2L)
  assertTrue(s.toFloat().also { rf = it } == 2.0f)
  assertTrue(s.toDouble().also { rd = it } == 2.0)
  assertTrue(ms.toInt().also { ri = it } == 32767)

  assertTrue(ms.toLong().also { rl = it } == 32767L)
  assertTrue(ms.toFloat().also { rf = it } == 32767.0f)
  assertTrue(ms.toDouble().also { rd = it } == 32767.0)

  assertTrue(i.toLong().also { rl = it } == 3L)
  assertTrue(i.toFloat().also { rf = it } == 3.0F)
  assertTrue(i.toDouble().also { rd = it } == 3.0)

  assertTrue(mi.toLong().also { rl = it } == 2147483647L)
  assertTrue(mi.toDouble().also { rd = it } == 2.147483647E9)

  assertTrue(l.toFloat().also { rf = it } == 4f)
  assertTrue(l.toDouble().also { rd = it } == 4.0)

  assertTrue(ll.toDouble().also { rd = it } == 2.415919103E9)

  assertTrue(ml.toDouble().also { rd = it } == 9.223372036854776E18)

  assertTrue((f - 2.7).also { rd = it } < 1e-7f)

  assertTrue(mf.toDouble().also { rd = it } == 3.4028234663852886e+38)
}

private fun testTernaryAssignment() {
  val b = 1.toByte()
  val i = 2
  val l = 3L
  val f = 4f
  val d = 5.0

  // Just to avoid JSCompiler being unhappy about "suspicious code" when seeing a ternary that
  // always evaluates to true.
  val alwaysTrue = fieldLongFromInt == 456L

  // Below int promotes to int, if there is a long promote to long, if there is a float promote to
  // float, if there is a double promote to double.
  assertTrue((if (alwaysTrue) b else i).toInt() == 1)
  assertTrue((if (alwaysTrue) i else l).toLong() == 2L)
  assertTrue((if (alwaysTrue) l else f).toFloat() == 3.0f)
  assertTrue((if (alwaysTrue) l else d).toDouble() == 3.0)
}

private fun testVariableInitializerAssignment() {
  val varIntFromChar = 456.toChar().code
  val varLongFromInt: Long = 456
  val varFloatFromLong = 456f
  val varDoubleFromLong = 456.0

  assertTrue(varIntFromChar == 456)
  assertTrue(varLongFromInt == 456L)
  assertTrue(varFloatFromLong == 456f)
  assertTrue(varDoubleFromLong == 456.0)
}

private fun testStaticCoercions() {
  val max = 9223372036854775807L

  val f = 9223372036854775807f //  9.223372036854776E18
  assertTrue(f == max.toFloat())

  val d = 9223372036854775807.0 //  9.223372036854776E18
  assertTrue(d == max.toDouble())
}
