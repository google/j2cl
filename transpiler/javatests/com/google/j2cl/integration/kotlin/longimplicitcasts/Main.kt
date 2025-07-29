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
package longimplicitcasts

import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Test implicit cast insertion in long operations. */
open class LongHolder {
  // Initialized with not-a-long.
  var fieldLong: Long = 100

  constructor() {}

  constructor(newFieldLong: Long) {
    fieldLong = newFieldLong
  }

  class SubLongHolder(newFieldInt: Int) : LongHolder(newFieldInt.toLong())
}

fun compareLong(leftValue: Long, rightValue: Long) {
  assertTrue(leftValue == rightValue)
}

fun main(vararg unused: String) {
  var longHolder = LongHolder()
  assertTrue(longHolder.fieldLong == 100L)
  val fbyte: Byte = 11
  val fchar = 12.toChar()
  val fshort: Short = 13
  val fint = 14
  val flong: Long = 15
  val ffloat = 16f
  val fdouble = 17.0
  var tlong: Long = 0
  assertTrue(tlong == 0L)

  // Direct assignments from smaller types.
  tlong = fbyte.toLong()
  assertTrue(tlong == fbyte.toLong())
  tlong = fchar.code.toLong()
  assertTrue(tlong == fchar.code.toLong())
  tlong = fshort.toLong()
  assertTrue(tlong == fshort.toLong())
  tlong = fint.toLong()
  assertTrue(tlong == fint.toLong())
  tlong = flong
  assertTrue(tlong == flong)

  // Implicit casts to long when performing any assignment binary operation on a long and any
  // non-long type.

  // If implicit casts are not inserted then long library operations will receive invalid
  // parameters and result in a runtime error.
  tlong = fint.toLong()
  assertTrue(tlong == 14L)
  tlong += fint.toLong()
  assertTrue(tlong == 28L)
  tlong -= fint.toLong()
  assertTrue(tlong == 14L)
  tlong *= fint.toLong()
  assertTrue(tlong == 196L)
  tlong /= fint.toLong()
  assertTrue(tlong == 14L)
  tlong = tlong and fint.toLong()
  assertTrue(tlong == 14L)
  tlong = tlong or fint.toLong()
  assertTrue(tlong == 14L)
  tlong = tlong xor fint.toLong()
  assertTrue(tlong == 0L)
  tlong %= fint.toLong()
  assertTrue(tlong == 0L)
  tlong = tlong shl fint // Does not cast to long on right hand side.
  assertTrue(tlong == 0L)
  tlong = tlong shr fint // Does not cast to long on right hand side.
  assertTrue(tlong == 0L)
  tlong = tlong ushr fint // Does not cast to long on right hand side.
  assertTrue(tlong == 0L)

  // Implicit casts to long when performing the PLUS_EQUALS binary operation on a long and any
  // non-long type.
  tlong += fbyte.toLong()
  assertTrue(tlong == 11L)
  tlong += fchar.code.toLong()
  assertTrue(tlong == 23L)
  tlong += fshort.toLong()
  assertTrue(tlong == 36L)
  tlong += fint.toLong()
  assertTrue(tlong == 50L)
  tlong += flong
  assertTrue(tlong == 65L)
  tlong += ffloat.toLong()
  assertTrue(tlong == 81L)
  tlong += fdouble.toLong()
  assertTrue(tlong == 98L)

  // Implicit casts to long when performing any non assignment binary operation on a long and any
  // non-long type.
  tlong = flong * fint
  assertTrue(tlong == 210L)
  tlong = flong / fint
  assertTrue(tlong == 1L)
  tlong = flong % fint
  assertTrue(tlong == 1L)
  tlong = flong + fint
  assertTrue(tlong == 29L)
  tlong = flong - fint
  assertTrue(tlong == 1L)
  tlong = flong shl fint // Does not cast to long on right hand side.
  assertTrue(tlong == 245760L)
  tlong = flong shr fint // Does not cast to long on right hand side.
  assertTrue(tlong == 0L)
  tlong = flong ushr fint // Does not cast to long on right hand side.
  assertTrue(tlong == 0L)
  // tlong = flong < fint; // illegal
  // tlong = flong > fint; // illegal
  // tlong = flong <= fint; // illegal
  // tlong = flong >= fint; // illegal
  // tlong = flong == fint; // illegal
  // tlong = flong != fint; // illegal
  tlong = flong xor fint.toLong()
  assertTrue(tlong == 1L)
  tlong = flong and fint.toLong()
  assertTrue(tlong == 14L)
  tlong = flong or fint.toLong()
  assertTrue(tlong == 15L)

  // Implicit casts to long when performing the PLUS binary operation on a long and any non-long
  // type.
  tlong = flong + fbyte
  assertTrue(tlong == 26L)
  tlong = flong + fchar.code.toLong()
  assertTrue(tlong == 27L)
  tlong = flong + fshort
  assertTrue(tlong == 28L)
  tlong = flong + fint
  assertTrue(tlong == 29L)
  tlong = flong + flong
  assertTrue(tlong == 30L)

  // Implicit casts to long when passing into a long parameter slot.
  compareLong(fbyte.toLong(), fbyte.toLong())
  assertTrue(LongHolder(fbyte.toLong()).fieldLong == fbyte.toLong())
  compareLong(fchar.code.toLong(), fchar.code.toLong())
  assertTrue(LongHolder(fchar.toLong()).fieldLong == fchar.code.toLong())
  compareLong(fshort.toLong(), fshort.toLong())
  assertTrue(LongHolder(fshort.toLong()).fieldLong == fshort.toLong())
  compareLong(fint.toLong(), fint.toLong())
  assertTrue(LongHolder(fint.toLong()).fieldLong == fint.toLong())
  compareLong(flong, flong)
  assertTrue(LongHolder(flong).fieldLong == flong)
  // compareLong(ffloat, (long)ffloat); // illegal
  // assertTrue(new LongHolder(ffloat).fieldLong == (long) ffloat); // illegal
  // compareLong(fdouble, (long)fdouble); // illegal
  // assertTrue(new LongHolder(fdouble).fieldLong == (long) fdouble); // illegal

  // To show that coercions occur even in super() params.
  assertTrue(LongHolder.SubLongHolder(fint).fieldLong == fint.toLong())

  // If implicit casts away from long are not inserted then long library operations will receive
  // invalid parameters and result in a runtime error.
  tlong = flong shl flong.toInt()
  assertTrue(tlong == 491520L)
  tlong = flong shr flong.toInt()
  assertTrue(tlong == 0L)
  tlong = flong ushr flong.toInt()
  assertTrue(tlong == 0L)
  tlong = tlong shl flong.toInt()
  assertTrue(tlong == 0L)
  tlong = tlong shr flong.toInt()
  assertTrue(tlong == 0L)
  tlong = tlong ushr flong.toInt()
  assertTrue(tlong == 0L)

  assertTrue(Long.MAX_VALUE.toDouble() != 9223372036854776833.0)

  // Long arrays
  var i = 0
  val longArray = longArrayOf(0L, 0L, 0L)
  longArray[i++]++
  assertTrue(i == 1)
}
