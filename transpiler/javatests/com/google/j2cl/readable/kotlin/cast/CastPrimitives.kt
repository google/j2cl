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
package cast

class CastPrimitives {
  fun testPrimitiveCasts() {
    var b: Byte = 1
    var c = 1.toChar()
    var s: Short = 1
    var i = 1
    var l = 1L
    var f = 1.1f
    var d = 1.1

    b = b.toByte()
    c = b.toChar()
    s = b.toShort()
    i = b.toInt()
    l = b.toLong()
    f = b.toFloat()
    d = b.toDouble()

    b = c.toByte()
    c = c.toChar()
    s = c.toShort()
    i = c.toInt()
    l = c.toLong()
    f = c.toFloat()
    d = c.toDouble()

    b = s.toByte()
    c = s.toChar()
    i = s.toInt()
    s = s.toShort()
    l = s.toLong()
    f = s.toFloat()
    d = s.toDouble()

    b = i.toByte()
    c = i.toChar()
    i = i.toInt()
    s = i.toShort()
    l = i.toLong()
    f = i.toFloat()
    d = i.toDouble()

    b = l.toByte()
    c = l.toChar()
    i = l.toInt()
    s = l.toShort()
    l = l.toLong()
    f = l.toFloat()
    d = l.toDouble()

    b = f.toInt().toByte()
    c = f.toChar()
    i = f.toInt()
    s = f.toInt().toShort()
    l = f.toLong()
    f = f.toFloat()
    d = f.toDouble()

    b = d.toInt().toByte()
    c = d.toChar()
    i = d.toInt()
    s = d.toInt().toShort()
    l = d.toLong()
    f = d.toFloat()
    d = d.toDouble()
  }

  fun testReferenceToPrimitive() {
    val o = Any()
    val bool = o as Boolean
    val b = o as Byte
    val c = o as Char
    val s = o as Short
    val i = o as Int
    val l = o as Long
    val f = o as Float
    val d = o as Double
  }

  fun testLiteralToPrimitive() {
    var b: Byte
    var c: Char
    var s: Short
    var i: Int
    var l: Long
    var f: Float
    var d: Double

    b = 1.toByte()
    c = 1.toChar()
    s = 1.toShort()
    i = 1.toInt()
    l = 1.toLong()
    f = 1.toFloat()
    d = 1.toDouble()

    b = 1L.toByte()
    c = 1L.toInt().toChar()
    s = 1L.toShort()
    i = 1L.toInt()
    l = 1L.toLong()
    f = 1L.toFloat()
    d = 1L.toDouble()

    b = 1.2f.toInt().toByte()
    c = 1.2f.toInt().toChar()
    s = 1.2f.toInt().toShort()
    i = 1.2f.toInt()
    l = 1.2f.toLong()
    f = 1.2f.toFloat()
    d = 1.2f.toDouble()

    b = 1.2.toInt().toByte()
    c = 1.2.toInt().toChar()
    s = 1.2.toInt().toShort()
    i = 1.2.toInt()
    l = 1.2.toLong()
    f = 1.2.toFloat()
    d = 1.2.toDouble()

    b = 'a'.code.toByte()
    c = 'a'.code.toChar()
    s = 'a'.code.toShort()
    i = 'a'.code
    l = 'a'.code.toLong()
    f = 'a'.code.toFloat()
    d = 'a'.code.toDouble()
  }

  fun testUnboxAndWiden() {
    val boxedByte: Byte? = 0.toByte()

    if (boxedByte is Byte) {
      // char c = (char) boxedByte; // illegal
      val s = boxedByte.toShort()
      val i = boxedByte.toInt()
      val l = boxedByte.toLong()
      val f = boxedByte.toFloat()
      val d = boxedByte.toDouble()
    }
  }

  // There is no implicit cast between primitives
  // fun testImplicitArgumentCasts() {
  //   val b: Byte = 127
  //   val c = 65535.toChar()
  //   val s: Short = 32767
  //   val i = 2147483647
  //   val l = 9223372036854775807L
  //   val f = 3.4028235E38f
  //   val d = 1.7976931348623157E308
  //
  //   getShort(b.toShort())
  //   getInt(b.toInt())
  //   getLong(b.toLong())
  //   getFloat(b.toFloat())
  //   getDouble(b.toDouble())
  //
  //   getFloat(l.toFloat())
  //   getDouble(l.toDouble())
  //   getDouble(f.toDouble())
  //
  //   // Do some of the same checks for NewInstance invocations.
  //   IntValueHolder(b.toInt())
  //   IntValueHolder(c.code)
  //   IntValueHolder(s.toInt())
  //
  //   LongValueHolder(b.toLong())
  //   LongValueHolder(c.code.toLong())
  //   LongValueHolder(i.toLong())
  // }
  //
  // private fun getShort(value: Short): Short {
  //   return value
  // }
  //
  // private fun getInt(value: Int): Int {
  //   return value
  // }
  //
  // private fun getLong(value: Long): Long {
  //   return value
  // }
  //
  // private fun getFloat(value: Float): Float {
  //   return value
  // }
  //
  // private fun getDouble(value: Double): Double {
  //   return value
  // }
  //
  // private class IntValueHolder internal constructor(value: Int)
  // private class LongValueHolder internal constructor(value: Long)

  fun testImplicitLongAssignmentCasts() {
    val fbyte: Byte = 11
    val fchar = 12.toChar()
    val fshort: Short = 13
    val fint = 14
    val flong: Long = 15
    val ffloat = 16f
    val fdouble = 17.0

    // Initialized with not-a-long.
    var tlong: Long = 0

    // No implicit cast on assignment in Kotlin
    // Direct assignments from smaller types.
    // tlong = fbyte.toLong()
    // tlong = flong

    // There is no implicit cast on compound assignment in Kotlin
    // Implicit casts to long when performing any assignment binary operation on a long and any
    // non-long type.
    // tlong = fint.toLong()
    // tlong += fint.toLong()
    // tlong = tlong shl fint // Does not cast to long on right hand side.

    // There is no implicit cast on compound assignment in Kotlin
    // Implicit casts to long when performing the PLUS_EQUALS binary operation on a long and any
    // non-long type.
    // tlong += fchar.code.toLong()
    // tlong += flong
    // tlong += ffloat.toLong()

    // Note that in Kotlin there is no implicit cast: The plus() operator function has different
    // overloads to accept all primitives type.
    // Implicit casts to long when performing any non assignment binary operation on a long and any
    // non-long type.
    tlong = flong * fint
    tlong = flong shr fint // Does not cast to long on right hand side.

    // Implicit casts to long when performing the PLUS binary operation on a long and any non-long
    // type.
    tlong = flong + fshort
    tlong = flong + flong

    // Not allowed in Kotlin. rhd need to be Long
    // Bit shift operations should coerce the right hand side to int (NOT long).
    // tlong = flong shl tlong.toInt()
    // tlong = tlong shl flong.toInt()

    // Repro for b/67599510
    tlong = 0 + 1 + 2L
  }
}
