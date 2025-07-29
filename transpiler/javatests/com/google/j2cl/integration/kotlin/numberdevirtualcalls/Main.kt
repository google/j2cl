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
package numberdevirtualcalls

import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  val b: Number = 1.toByte()
  assertTrue(b.toByte() == 1.toByte())
  assertTrue(b.toDouble() == 1.0)
  assertTrue(b.toFloat() == 1.0f)
  assertTrue(b.toInt() == 1)
  assertTrue(b.toLong() == 1L)
  assertTrue(b.toShort() == 1.toShort())

  val mb: Byte? = 127.toByte()
  assertTrue(mb == 127.toByte())
  assertTrue(mb!!.toDouble() == 127.0)
  assertTrue(mb.toFloat() == 127.0f)
  assertTrue(mb.toInt() == 127)
  assertTrue(mb.toLong() == 127L)
  assertTrue(mb.toShort() == 127.toShort())

  val d: Number = 1.1
  assertTrue(d.toByte() == 1.toByte())
  assertTrue(d.toDouble() == 1.1)
  // assertTrue((d.floatValue() == 1.1f)); // float is emitted as 1.100000023841858
  assertTrue(d.toInt() == 1)
  assertTrue(d.toLong() == 1L)
  assertTrue(d.toShort() == 1.toShort())

  val md: Double? = 1.7976931348623157E308
  assertTrue(md!!.toInt().toByte() == (-1).toByte())
  assertTrue(md == 1.7976931348623157E308)
  assertTrue(md.toInt() == 2147483647)
  assertTrue(md.toLong() == 9223372036854775807L)
  assertTrue(md.toInt().toShort() == (-1).toShort())

  val f: Number = 1.1f
  assertTrue(f.toByte() == 1.toByte())
  assertTrue(f.toDouble() - 1.1 < 1e-7)
  assertTrue(f.toFloat() == 1.1f)
  assertTrue(f.toInt() == 1)
  assertTrue(f.toLong() == 1L)
  assertTrue(f.toShort() == 1.toShort())

  val mf: Float? = 3.4028235E38f
  assertTrue(mf!!.toInt().toByte() == (-1).toByte())
  assertTrue(mf == 3.4028234663852886E38f)
  assertTrue(mf.toInt() == 2147483647)
  assertTrue(mf.toLong() == 9223372036854775807L)
  assertTrue(mf.toInt().toShort() == (-1).toShort())

  val i: Number = 1
  assertTrue(i.toByte() == 1.toByte())
  assertTrue(i.toDouble() == 1.0)
  assertTrue(i.toFloat() == 1.0f)
  assertTrue(i.toInt() == 1)
  assertTrue(i.toLong() == 1L)
  assertTrue(i.toShort() == 1.toShort())

  val mi: Int? = 2147483647
  assertTrue(mi!!.toByte() == (-1).toByte())
  assertTrue(mi.toDouble() == 2.147483647E9)
  assertTrue(mi == 2147483647)
  assertTrue(mi.toLong() == 2147483647L)
  assertTrue(mi.toShort() == (-1).toShort())

  val l: Number = 1L
  assertTrue(l.toByte() == 1.toByte())
  assertTrue(l.toDouble() == 1.0)
  assertTrue(l.toFloat() == 1.0f)
  assertTrue(l.toInt() == 1)
  assertTrue(l.toLong() == 1L)
  assertTrue(l.toShort() == 1.toShort())

  val ml: Long? = 9223372036854775807L
  assertTrue(ml!!.toByte() == (-1).toByte())
  assertTrue(ml.toDouble() == 9.223372036854776E18)
  assertTrue(ml.toFloat() == 9.223372E18f)
  assertTrue(ml.toInt() == -1)
  assertTrue(ml == 9223372036854775807L)
  assertTrue(ml.toShort() == (-1).toShort())

  val s: Number = 1.toShort()
  assertTrue(s.toByte() == 1.toByte())
  assertTrue(s.toDouble() == 1.0)
  assertTrue(s.toFloat() == 1.0f)
  assertTrue(s.toInt() == 1)
  assertTrue(s.toLong() == 1L)
  assertTrue(s.toShort() == 1.toShort())

  val ms: Short? = 32767.toShort()
  assertTrue(ms!!.toByte() == (-1).toByte())
  assertTrue(ms.toDouble() == 32767.0)
  assertTrue(ms.toFloat() == 32767.0f)
  assertTrue(ms.toInt() == 32767)
  assertTrue(ms.toLong() == 32767L)
  assertTrue(ms.toShort() == 32767.toShort())

  val c: Char = 'a'
  assertTrue(c == 'a')
  assertTrue(c.toChar() == 'a')
  assertTrue(c.toInt() == 97)
  assertTrue(c.toShort() == 97.toShort())
  assertTrue(c.toByte() == 97.toByte())
  assertTrue(c.toLong() == 97L)
  assertTrue(c.toDouble() == 97.0)
  assertTrue(c.toFloat() == 97f)

  var bc: Char? = null
  assertTrue(bc == null)
  assertTrue(bc?.toChar() == null)
  assertTrue(bc?.toInt() == null)
  assertTrue(bc?.toShort() == null)
  assertTrue(bc?.toByte() == null)
  assertTrue(bc?.toLong() == null)
  assertTrue(bc?.toDouble() == null)
  assertTrue(bc?.toFloat() == null)

  bc = 'a'
  assertTrue(bc == 'a')
  assertTrue(bc?.toChar() == 'a')
  assertTrue(bc?.toInt() == 97)
  assertTrue(bc?.toShort() == 97.toShort())
  assertTrue(bc?.toByte() == 97.toByte())
  assertTrue(bc?.toLong() == 97L)
  assertTrue(bc?.toDouble() == 97.0)
  assertTrue(bc?.toFloat() == 97f)

  val bool: Boolean? = true
  assertTrue(bool!!)
}
