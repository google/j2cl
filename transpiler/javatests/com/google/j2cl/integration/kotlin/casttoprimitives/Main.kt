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
package casttoprimitives

import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testPrimitiveToPrimitive()
  testObjectReferenceToPrimitive()
  testBoxedToPrimitive()
  testTypeExtendsLongReferenceToPrimitive<Long>()
  testTypeExtendsIntersectionReferenceToPrimitive<Long>()
  testPrimitiveToReference()
}

// Constants that can not be expressed as literals.
private val BYTE_MINUS_ONE: Byte = -1
private val BYTE_ZERO: Byte = 0
private val BYTE_ONE: Byte = 1
private val BYTE_TWO: Byte = 2
private val BYTE_THREE: Byte = 3
private val BYTE_FOUR: Byte = 4
private val BYTE_97: Byte = 97
private val SHORT_MINUS_ONE: Short = -1
private val SHORT_ZERO: Short = 0
private val SHORT_ONE: Short = 1
private val SHORT_TWO: Short = 2
private val SHORT_THREE: Short = 3
private val SHORT_FOUR: Short = 4
private val SHORT_97: Short = 97
private val SHORT_127: Short = 127

fun testPrimitiveToPrimitive() {
  val b: Byte = 1
  val maxByte: Byte = 127 // Byte.MAX_VALUE
  val c: Char = 'a'
  val maxChar: Char = '\uFFFF' // Character.MAX_VALUE;
  val s: Short = 2
  val maxShort: Short = 32767 // Short.MAX_VALUE
  val i: Int = 3
  val maxInt: Int = 2147483647 // Integer.MAX_VALUE
  val l: Long = 4L
  val ll: Long = 2415919103L // max_int < ll < max_int * 2, used for testing for signs.
  val maxLong: Long = 9223372036854775807L // Long.MAX_VALUE;
  val f: Float = 2.7f
  val maxFloat: Float = 3.4028235E38f // Float.MAX_VALUE;
  val posInfiniteFloat: Float = Float.POSITIVE_INFINITY
  val negInfiniteFloat: Float = Float.NEGATIVE_INFINITY
  val nanFloat: Float = Float.NaN
  val d: Double = 2.6
  val dd: Double = 2415919103.7 // dd > max_int
  val maxDouble: Double = 1.7976931348623157E308 // Double.MAX_VALUE;
  val posInfiniteDouble: Double = Double.POSITIVE_INFINITY
  val negInfiniteDouble: Double = Double.NEGATIVE_INFINITY
  val nanDouble: Double = Double.NaN

  assertTrue(b.toChar() == '\u0001')
  assertTrue(b.toShort() == SHORT_ONE)
  assertTrue(b.toInt() == 1)
  assertTrue(b.toLong() == 1L)
  assertTrue(b.toFloat() == 1.0f)
  assertTrue(b.toDouble() == 1.0)

  assertTrue(maxByte.toChar() == '\u007F')
  assertTrue(maxByte.toShort() == SHORT_127)
  assertTrue(maxByte.toInt() == 127)
  assertTrue(maxByte.toLong() == 127L)
  assertTrue(maxByte.toFloat() == 127.0f)
  assertTrue(maxByte.toDouble() == 127.0)

  assertTrue(c.toByte() == BYTE_97)
  assertTrue(c.toShort() == SHORT_97)
  assertTrue(c == '\u0061')
  assertTrue(c.toLong() == 97L)
  assertTrue(c.toFloat() == 97.0f)
  assertTrue(c.toDouble() == 97.0)
  assertTrue(c.toChar() == 'a')

  assertTrue(maxChar.toByte() == BYTE_MINUS_ONE)
  assertTrue(maxChar.toShort() == SHORT_MINUS_ONE)
  assertTrue(maxChar.toInt() == 65535)
  assertTrue(maxChar.toLong() == 65535L)
  assertTrue(maxChar.toFloat() == 65535.0f)
  assertTrue(maxChar.toDouble() == 65535.0)

  assertTrue(s.toByte() == BYTE_TWO)
  assertTrue(s.toChar() == '\u0002')
  assertTrue(s.toInt() == 2)
  assertTrue(s.toLong() == 2L)
  assertTrue(s.toFloat() == 2.0f)
  assertTrue(s.toDouble() == 2.0)

  assertTrue(maxShort.toByte() == BYTE_MINUS_ONE)
  assertTrue(maxShort.toChar() == '\u7FFF')
  assertTrue(maxShort.toInt() == 32767)
  assertTrue(maxShort.toLong() == 32767L)
  assertTrue(maxShort.toFloat() == 32767.0f)
  assertTrue(maxShort.toDouble() == 32767.0)

  assertTrue(i.toByte() == BYTE_THREE)
  assertTrue(i.toChar() == '\u0003')
  assertTrue(i.toShort() == SHORT_THREE)
  assertTrue(i.toLong() == 3L)
  assertTrue(i.toFloat() == 3.0f)
  assertTrue(i.toDouble() == 3.0)

  assertTrue(maxInt.toByte() == BYTE_MINUS_ONE)
  assertTrue(maxInt.toChar() == '\uFFFF')
  assertTrue(maxInt.toShort() == SHORT_MINUS_ONE)
  assertTrue(maxInt.toLong() == 2147483647L)
  // assertTrue(maxInt.toFloat() == 2.14748365E9f)); // toFloat() returns double precision
  // 2.147483647E9
  assertTrue(maxInt.toDouble() == 2.147483647E9)

  assertTrue(l.toByte() == BYTE_FOUR)
  assertTrue(l.toChar() == '\u0004')
  assertTrue(l.toShort() == SHORT_FOUR)
  assertTrue(l.toInt() == 4)
  assertTrue(l.toFloat() == 4f)
  assertTrue(l.toDouble() == 4.0)

  assertTrue(ll.toByte() == BYTE_MINUS_ONE)
  assertTrue(ll.toChar() == '\uFFFF')
  assertTrue(ll.toShort() == SHORT_MINUS_ONE)
  assertTrue(ll.toInt() == -1879048193)
  // assertTrue(ll.toFloat() == 2.4159191E9)); // toFloat() returns double-precision 2.415919103E9.
  assertTrue(ll.toDouble() == 2.415919103E9)

  assertTrue(maxLong.toByte() == BYTE_MINUS_ONE)
  assertTrue(maxLong.toChar() == '\uFFFF')
  assertTrue(maxLong.toShort() == SHORT_MINUS_ONE)
  assertTrue(maxLong.toInt() == -1)
  assertTrue(maxLong.toFloat() == 9.223372036854776E18f)
  assertTrue(maxLong.toDouble() == 9.223372036854776E18)

  assertTrue(f.toInt().toByte() == BYTE_TWO)
  assertTrue(f.toInt().toChar() == '\u0002')
  assertTrue(f.toInt().toShort() == SHORT_TWO)
  assertTrue(f.toInt() == 2)
  assertTrue(f.toLong() == 2L)
  assertTrue(f.toDouble() - 2.7 < 1e-7)

  assertTrue(maxFloat.toInt().toByte() == BYTE_MINUS_ONE)
  assertTrue(maxFloat.toChar() == '\uFFFF')
  assertTrue(maxFloat.toInt().toShort() == SHORT_MINUS_ONE)
  assertTrue(maxFloat.toInt() == 2147483647)
  assertTrue(maxFloat.toLong() == 9223372036854775807L)
  assertTrue(maxFloat.toDouble() == 3.4028234663852886E38)

  assertTrue(posInfiniteFloat.toInt().toByte() == BYTE_MINUS_ONE)
  assertTrue(posInfiniteFloat.toInt().toShort() == SHORT_MINUS_ONE)
  assertTrue(posInfiniteFloat.toInt().toChar() == '\uFFFF')
  assertTrue(posInfiniteFloat.toInt() == 2147483647)
  assertTrue(posInfiniteFloat.toLong() == 9223372036854775807L)
  assertTrue(posInfiniteFloat.toDouble() == Double.POSITIVE_INFINITY)

  assertTrue(negInfiniteFloat.toInt().toByte() == BYTE_ZERO)
  assertTrue(negInfiniteFloat.toInt().toShort() == SHORT_ZERO)
  assertTrue(negInfiniteFloat.toInt().toChar() == 0.toChar())
  assertTrue(negInfiniteFloat.toInt() == -2147483648)
  assertTrue(negInfiniteFloat.toLong() == -9223372036854775807L - 1L)
  assertTrue(negInfiniteFloat.toDouble() == Double.NEGATIVE_INFINITY)

  assertTrue(nanFloat.toInt().toByte() == BYTE_ZERO)
  assertTrue(nanFloat.toInt().toShort() == SHORT_ZERO)
  assertTrue(nanFloat.toInt().toChar() == '\u0000')
  assertTrue(nanFloat.toInt() == 0)
  assertTrue(nanFloat.toLong() == 0L)
  assertTrue(java.lang.Double.isNaN(nanFloat.toDouble()))

  assertTrue(d.toInt().toByte() == BYTE_TWO)
  assertTrue(d.toInt().toChar() == '\u0002')
  assertTrue(d.toInt().toShort() == SHORT_TWO)
  assertTrue(d.toInt() == 2)
  assertTrue(d.toLong() == 2L)
  // assertTrue((d.toFloat() == 2.6f)); // float is emitted as 2.5999999046325684

  assertTrue(dd.toInt().toByte() == BYTE_MINUS_ONE)
  assertTrue(dd.toInt().toChar() == '\uFFFF')
  assertTrue(dd.toInt().toShort() == SHORT_MINUS_ONE)
  assertTrue(dd.toInt() == 2147483647)
  assertTrue(dd.toLong() == 2415919103L)
  // assertTrue((dd.toFloat() == 2.4159191E9)); // toFloat() returns double-precision 2415919103.7

  assertTrue(maxDouble.toInt().toByte() == BYTE_MINUS_ONE)
  assertTrue(maxDouble.toInt().toChar() == '\uFFFF')
  assertTrue(maxDouble.toInt().toShort() == SHORT_MINUS_ONE)
  assertTrue(maxDouble.toInt() == 2147483647)
  assertTrue(maxDouble.toLong() == 9223372036854775807L)

  assertTrue(posInfiniteDouble.toInt().toByte() == BYTE_MINUS_ONE)
  assertTrue(posInfiniteDouble.toInt().toShort() == SHORT_MINUS_ONE)
  assertTrue(posInfiniteDouble.toInt().toChar() == '\uFFFF')
  assertTrue(posInfiniteDouble.toInt() == 2147483647)
  assertTrue(posInfiniteDouble.toLong() == 9223372036854775807L)
  assertTrue(posInfiniteDouble.toFloat() == Float.POSITIVE_INFINITY)

  assertTrue(negInfiniteDouble.toInt().toByte() == BYTE_ZERO)
  assertTrue(negInfiniteDouble.toInt().toShort() == SHORT_ZERO)
  assertTrue(negInfiniteDouble.toInt().toChar() == '\u0000')
  assertTrue(negInfiniteDouble.toInt() == -2147483648)
  assertTrue(negInfiniteDouble.toLong() == -9223372036854775807L - 1L)
  assertTrue(negInfiniteDouble.toFloat() == Float.NEGATIVE_INFINITY)

  assertTrue(nanDouble.toInt().toByte() == BYTE_ZERO)
  assertTrue(nanDouble.toInt().toShort() == SHORT_ZERO)
  assertTrue(nanDouble.toInt().toChar() == '\u0000')
  assertTrue(nanDouble.toInt() == 0)
  assertTrue(nanDouble.toLong() == 0L)
  assertTrue(java.lang.Float.isNaN(nanDouble.toFloat()))
}

fun testObjectReferenceToPrimitive() {
  val o: Any? = Any()
  assertThrowsClassCastException {
    val b = o as Boolean
  }
  assertThrowsClassCastException {
    val b = o as Byte
  }
  assertThrowsClassCastException {
    val c = o as Char
  }
  assertThrowsClassCastException {
    val s = o as Short
  }
  assertThrowsClassCastException {
    val i = o as Int
  }
  assertThrowsClassCastException {
    val l = o as Long
  }
  assertThrowsClassCastException {
    val f = o as Float
  }
  assertThrowsClassCastException {
    val d = o as Double
  }

  var o1: Any? = false
  val bool = o1 as Boolean
  assertTrue(!bool)

  o1 = Byte.MAX_VALUE
  assertTrue(o1.toInt() == 127)

  o1 = 'a'
  assertTrue(o1 == 'a')

  o1 = Short.MAX_VALUE
  assertTrue(o1.toInt() == 32767)

  o1 = 1
  assertTrue(o1 == 1)

  o1 = 1L
  assertTrue(o1 == 1L)

  o1 = 1.1f
  assertTrue(o1 == 1.1f)

  o1 = 1.2
  assertTrue(o1 == 1.2)
}

fun <T : Long?> testTypeExtendsLongReferenceToPrimitive() {
  val o = 1L as T
  val l = o as Long
  assertTrue(l == 1L)
  val f = o.toFloat()
  assertTrue(f == 1.0f)
  val d = o.toDouble()
  assertTrue(d == 1.0)
}

fun <T> testTypeExtendsIntersectionReferenceToPrimitive() where T : Long?, T : Comparable<Long>? {
  val o = 1L as T
  val l = o as Long
  assertTrue(l == 1L)
  val f = o.toFloat()
  assertTrue(f == 1.0f)
  val d = o.toDouble()
  assertTrue(d == 1.0)
}

fun testBoxedToPrimitive() {
  val b = BYTE_ONE
  val c = 'a'
  val s = SHORT_ONE
  val i = 1
  val l = 1L
  val f = 1.1f
  val d = 1.1

  val ss = b.toShort()
  assertTrue(ss.toInt() == 1)

  var ii = b.toInt()
  assertTrue(ii == 1)
  ii = c.toInt()
  assertTrue(ii == 97)
  ii = s.toInt()
  assertTrue(ii == 1)

  var ll = b.toLong()
  assertTrue(ll == 1L)
  ll = s.toLong()
  assertTrue(ll == 1L)
  ll = c.toInt().toLong()
  assertTrue(ll == 97L)
  ll = i.toLong()
  assertTrue(ll == 1L)

  var ff = b.toFloat()
  assertTrue(ff == 1f)
  ff = s.toFloat()
  assertTrue(ff == 1f)
  ff = c.toInt().toFloat()
  assertTrue(ff == 97f)
  ff = i.toFloat()
  assertTrue(ff == 1f)
  ff = l.toFloat()
  assertTrue(ff == 1f)

  var dd = b.toDouble()
  assertTrue(dd == 1.0)
  dd = s.toDouble()
  assertTrue(dd == 1.0)
  dd = c.toInt().toDouble()
  assertTrue(dd == 97.0)
  dd = i.toDouble()
  assertTrue(dd == 1.0)
  dd = l.toDouble()
  assertTrue(dd == 1.0)
  dd = f.toDouble()
  assertTrue(dd - 1.1 < 1e-7)
}

fun testPrimitiveToReference() {
  val bool = true
  val b: Byte = 1
  val c = 'a'
  val s: Short = 1
  val i = 1
  val l = 1L
  val f = 1.0f
  val d = 1.0
  var o: Any = bool
  assertTrue(o != null)
  o = b
  assertTrue(o != null)
  o = c
  assertTrue(o != null)
  o = s
  assertTrue(o != null)
  o = i
  assertTrue(o != null)
  o = l
  assertTrue(o != null)
  o = f
  assertTrue(o != null)
  o = d
  assertTrue(o != null)
  o = bool
  assertTrue(o != null)
  o = b
  assertTrue(o != null)
  o = c
  assertTrue(o != null)
  o = s
  assertTrue(o != null)
  o = i
  assertTrue(o != null)
  o = l
  assertTrue(o != null)
  o = f
  assertTrue(o != null)
  o = d
  assertTrue(o != null)
}
