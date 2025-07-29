/*
 * Copyright 2023 Google Inc.
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
package narrowingprimitiveconversion

import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testCoercions()
  testStaticCoercions()
}

private fun testCoercions() {
  val b: Byte = 1
  val mb: Byte = 127 // Byte.MAX_VALUE
  val c = 'a'
  val mc: Char = 65535.toChar() // Character.MAX_VALUE;
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
  val dd = 2415919103.7 // dd > max_int
  val md = 1.7976931348623157E308 // Double.MAX_VALUE;

  assertTrue(b.toChar() == '\u0001')

  assertTrue(mb.toChar().toInt() == 127)

  assertTrue(c.toByte() == 97.toByte())
  assertTrue(c.toShort() == 97.toShort())

  assertTrue(mc.toByte() == (-1).toByte())
  assertTrue(mc.toShort() == (-1).toShort())

  assertTrue(s.toByte() == 2.toByte())
  assertTrue(s.toChar().toInt() == 2)

  assertTrue(ms.toByte() == (-1).toByte())
  assertTrue(ms.toChar().toInt() == 32767)

  assertTrue(i.toByte() == 3.toByte())
  assertTrue(i.toChar().toInt() == 3)
  assertTrue(i.toShort() == 3.toShort())

  assertTrue(mi.toByte() == (-1).toByte())
  assertTrue(mi.toChar().toInt() == 65535)
  assertTrue(mi.toShort() == (-1).toShort())

  assertTrue(l.toByte() == 4.toByte())
  assertTrue(l.toChar().toInt() == 4)
  assertTrue(l.toShort() == 4.toShort())
  assertTrue(l.toInt() == 4)

  assertTrue(ll.toByte() == (-1).toByte())
  assertTrue(ll.toChar().toInt() == 65535)
  assertTrue(ll.toShort() == (-1).toShort())
  assertTrue(ll.toInt() == -1879048193)

  assertTrue(ml.toByte() == (-1).toByte())
  assertTrue(ml.toChar().toInt() == 65535)
  assertTrue(ml.toShort() == (-1).toShort())
  assertTrue(ml.toInt() == -1)

  assertTrue(f.toInt().toByte() == 2.toByte())
  assertTrue(f.toChar().toInt() == 2)
  assertTrue(f.toInt().toShort() == 2.toShort())
  assertTrue(f.toInt() == 2)
  assertTrue(f.toLong() == 2L)

  assertTrue(mf.toInt().toByte() == (-1).toByte())
  assertTrue(mf.toChar().toInt() == 65535)
  assertTrue(mf.toInt().toShort() == (-1).toShort())
  assertTrue(mf.toInt() == 2147483647)
  assertTrue(mf.toLong() == 9223372036854775807L)

  assertTrue(d.toInt().toByte() == 2.toByte())
  assertTrue(d.toChar().toInt() == 2)
  assertTrue(d.toInt().toShort() == 2.toShort())
  assertTrue(d.toInt() == 2)
  assertTrue(d.toLong() == 2L)

  assertTrue(dd.toInt().toByte() == (-1).toByte())
  assertTrue(dd.toChar().toInt() == 65535)
  assertTrue(dd.toInt().toShort() == (-1).toShort())
  assertTrue(dd.toInt() == 2147483647)
  assertTrue(dd.toLong() == 2415919103L)

  assertTrue(md.toInt().toByte() == (-1).toByte())
  assertTrue(md.toChar().toInt() == 65535)
  assertTrue(md.toInt().toShort() == (-1).toShort())
  assertTrue(md.toInt() == 2147483647)
  assertTrue(md.toLong() == 9223372036854775807L)

  val n = 5
  assertTrue(2 * (n / 2) == 4)

  // JLS 5.1.4
  val fmin = java.lang.Float.NEGATIVE_INFINITY
  val fmax = java.lang.Float.POSITIVE_INFINITY
  val fnan = java.lang.Float.NaN

  assertTrue(fmin.toLong() == java.lang.Long.MIN_VALUE)
  assertTrue(fmax.toLong() == java.lang.Long.MAX_VALUE)
  assertTrue(fnan.toLong() == 0L)

  assertTrue(fmin.toInt() == Integer.MIN_VALUE)
  assertTrue(fmax.toInt() == Integer.MAX_VALUE)
  assertTrue(fnan.toInt() == 0)

  assertTrue(fmin.toInt().toShort() == Integer.MIN_VALUE.toShort())
  assertTrue(fmax.toInt().toShort() == Integer.MAX_VALUE.toShort())
  assertTrue(fnan.toInt().toShort() == 0.toShort())

  assertTrue(fmin.toInt().toByte() == Integer.MIN_VALUE.toByte())
  assertTrue(fmax.toInt().toByte() == Integer.MAX_VALUE.toByte())
  assertTrue(fnan.toInt().toByte() == 0.toByte())

  assertTrue(fmin.toChar() == Integer.MIN_VALUE.toChar())
  assertTrue(fmax.toChar() == Integer.MAX_VALUE.toChar())
  assertTrue(fnan.toChar() == 0.toChar())

  val three: Byte = 3
  assertTrue(-128 == (three.toInt() shl 7).toByte().toInt())
}

private fun testStaticCoercions() {
  val max = 9223372036854775807L

  val b = 9223372036854775807L.toByte() // -1
  assertTrue(b == max.toByte())

  val c = 9223372036854775807L.toChar() // 65535
  assertTrue(c == max.toChar())

  val s = 9223372036854775807L.toShort() // -1
  assertTrue(s == max.toShort())

  val i = 9223372036854775807L.toInt() // -1
  assertTrue(i == max.toInt())

  val f = 9223372036854775807L.toFloat() //  9.223372036854776E18
  assertTrue(f == max.toFloat())

  val d = 9223372036854775807L.toDouble() //  9.223372036854776E18
  assertTrue(d == max.toDouble())
}
