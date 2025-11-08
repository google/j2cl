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
package compiletimeconstant

class CompileTimeConstant {
  companion object {
    val OBJ: Any? = null
    const val DEFAULT: Int = 0
    const val A = 10
    const val B = 20
    const val C = A * B
    const val D = "Tur\"tle"
    const val E = "Do\'ve"
    const val F = D + E
    const val J = F + F
    val K: String? = null
    const val L = "ThisIsALongString"
    const val M = "ThisIsALongStringAlso"
    const val N = "ThisIsALongStringAlsoButLonger"
    // const val O = 1 + " is 1" // Valid in Java, but not in Kotlin.
    const val G = 10000L
    const val H = 'A'
    const val I = G > 100
    const val P = 3.141592653589793
    const val Q = (P).toFloat()
    // TODO(b/232419907): Enable when unsigned types are emulated in the stdlib.
    // const val U = 1u

    const val MIN_BYTE: Byte = -128
    const val MIN_SHORT: Short = -32768

    const val MIN_BYTE_WITH_CAST = (-128).toByte()
    const val MIN_SHORT_WITH_CAST = (-32768).toShort()
  }

  val A2 = 10
  val B2 = 20
  val C2 = A * B
  val D2 = "Tur\"tle"
  val E2 = "Do\"ve"
  val F2 = D2 + E2
  val G2 = 10000L
  val H2 = 'A'
  val I2 = G2 > 100
  val CHAR_MAX_INT = Char.MAX_VALUE.code
  val CHAR_MIN_INT = Char.MIN_VALUE.toInt()

  val b: Boolean
  val s: String
  val l: String
  val m: String
  val n: String

  init {
    b = I2
    s = G.toString() + F
    l = L
    m = M
    n = N

    // Constant expressions with overflow.
    // In Kotlin, `0x80000000` is considered a long literal.
    val intUnaryMinusOverflow: Int = -(0x80000000 as Int)
    val intPlusOverflow: Int = 0x7fffffff + 1
    val intMinusOverflow: Int = 0x80000000 as Int - 1
    val intTimesOverflow: Int = 0x7fffffff * 2
    val intShlOverflow: Int = 0x7fffffff shl 2
  }

  fun testInlineIntoNonConstExpressions() =
    when {
      D == "" + A -> 1
      D == E -> 2
      D == F -> 3
      E == F -> 4
      N == L -> 5
      M == L -> 6
      M == N -> 7
      else -> 8
    }

  fun testExpressionsWithIntrinsicConstEvaluation(code: Int) {
    // Char.code property and toByte/Short/Int/Long/Float/Double functions are marked with
    // IntrinsicConstEvaluation
    val CHAR_MAX_INT_VALUE = Char.MAX_VALUE.code
    val CHAR_MIN_INT_VALUE = Char.MIN_VALUE.toInt()
    val CHAR_MAX_BYTE_VALUE = Char.MAX_VALUE.code.toByte()
    val CHAR_MIN_BYTE_VALUE = Char.MIN_VALUE.code.toByte()
    val CHAR_MAX_SHORT_VALUE = Char.MAX_VALUE.code.toShort()
    val CHAR_MIN_SHORT_VALUE = Char.MIN_VALUE.code.toShort()
    val CHAR_MAX_LONG_VALUE = Char.MAX_VALUE.code.toLong()
    val CHAR_MIN_LONG_VALUE = Char.MIN_VALUE.code.toLong()
    val CHAR_MAX_FLOAT_VALUE = Char.MIN_VALUE.code.toFloat()
    val CHAR_MIN_FLOAT_VALUE = Char.MIN_VALUE.code.toFloat()
    val CHAR_MAX_DOUBLE_VALUE = Char.MAX_VALUE.toDouble()
    val CHAR_MIN_DOUBLE_VALUE = Char.MIN_VALUE.toDouble()

    // TODO(b/378892483): Re-enable this test when the IR deserializer is fixed.
    // // MAX_VALUE and MIN_VALUE of unsigned types uses contructors which are marked with
    // // IntrinsicConstEvaluation
    // val minUByteLimit: UByte = UByte.MIN_VALUE
    // if (minUByteLimit < UByte.MAX_VALUE) {}
    // val minUShortLimit: UShort = UShort.MIN_VALUE
    // if (minUShortLimit < UShort.MAX_VALUE) {}
    // val minUIntLimit: UInt = UInt.MIN_VALUE
    // if (minUIntLimit < UInt.MAX_VALUE) {}
    // val minULongLimit: ULong = ULong.MIN_VALUE
    // if (minULongLimit < ULong.MAX_VALUE) {}
  }
}
