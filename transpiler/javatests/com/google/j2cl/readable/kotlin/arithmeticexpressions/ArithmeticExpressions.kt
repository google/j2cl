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
 * See the License for the specific languagCS-ArithmeticExpressions-2022-08-12_115430e governing permissions and
 * limitations under the License.
 */
package arithmeticexpressions

private const val FLOAT_CONSTANT = 1.1f
private const val DOUBLE_CONSTANT = FLOAT_CONSTANT
private const val DOUBLE_CONSTANT_WITH_ARITHMETIC = FLOAT_CONSTANT + FLOAT_CONSTANT

fun testCoercions() {
  var b: Byte = 1L.toByte()
  var c: Char = 1L.toChar()
  var s: Short = 1L.toShort()
  var i: Int = 1L.toInt()
  var f: Float = 1L.toFloat()
  var d: Double = 1L.toDouble()
  b = 9223372036854775807L.toByte() // -1
  c = 9223372036854775807L.toChar() // 65535
  s = 9223372036854775807L.toShort() // -1
  i = 9223372036854775807L.toInt() // -1
  f = 9223372036854775807L.toFloat() //  9.223372036854776E18
  d = 9223372036854775807L.toDouble() //  9.223372036854776E18
  val o: Any = c
  s = (o as Char).toShort()
}

fun testPrimitives() {
  var a = 10
  val b = a++
  val c = a--
  val d = ++a
  val e = --a
  val f = -a
  val g = +a
  val h = a.inv()
  val i = 1 + 1 + 2 - 5
  val j = (1 + 2) * (3 + 4)
  val p = 1 / 2 * 3 % 4
  val r = -0x80000000
  val t = -(-(-1))
  val u = +(+(+1))
  val v = -(+(-1))
  val w = 5 - (-4)
  var k = !(1 + 2 + 3 == 4)
  val l = (1 + 2 != 4)
  val m = Long.MAX_VALUE != 9223372036854776833.0.toLong()
  val o: Double = ((5 + 1) / 2) - 0.0

  a = a shr 31
  a = a shr 1
  a += 1
  a /= 1
  a += Double.MAX_VALUE.toInt() // if not expanded and performed in double result will change.

  k = k or true

  var s: Short = 10
  k = ++s == 10.toShort()

  var q = 3 shr 2
  q = 3 ushr 2

  val x: Byte = ((a.toShort() + b.toShort().toByte()) * c.toByte().toShort()).toByte()

  val y = 'y'
  val z = 121L
  k = y.toLong() == z
}

fun testDoubleAssignments() {
  var a = 1.0
  a += 2.0
  a -= 3.0
  a *= 4.0
  a /= 5.0
  a %= 6.0
}

// Compount assignments in static fields
var one: Long = 1L
var foo = one++
val bar = foo++

fun testCompoundArray() {
  val ints = IntArray(0)
  ints[0] += 1
  ints[0] -= 1
  ints[0] *= 1
  ints[0] /= 1
  ints[0] %= 1
  ints[0]++
  ++ints[0]
  var i = 0
  ints[i++]++
  ++ints[++i]
  ints[i++] /= 1

  val longs = LongArray(0)
  // Missing the corresponding set method for array of longs. Trying to compile
  //     longs[0] += 1
  // gives "no set method providing array access"
  longs[0] += 1L
  longs[0]--
  --longs[0]
  getLongArray()[0]++

  val strings = null as Array<String?>?
  // strings[0] += null

  val shorts = ShortArray(0)
  val b = ++shorts[0] == 10.toShort()
}

fun testCompoundBoxedTypes() {
  class Ref<T>(var field: T)

  var ref = Ref<Byte>(1)
  ref.field++
  ++ref.field
  val n = 1 + ref.field

  var longRef = Ref(1L)
  longRef.field += 1
}

fun getLongArray(): LongArray {
  return LongArray(0)
}

class A {
  var intField: Int = 1
}

private fun getInteger(): Int? {
  return null
}

fun testSideEffect() {
  getWithSideEffect().intField += 5
}

fun getWithSideEffect(): A {
  return A()
}

var counter = 0

fun incrementCounter(): Int {
  return ++counter
}

fun testEquals() {
  val nullableDouble: Double? = 1.0
  val double = 1.0
  val nullableFloat: Float? = 1.0f
  val float = 1.0f

  // These are ieee754 semantics
  var b = nullableDouble == double
  b = double == nullableDouble
  b = double == double
  b = nullableDouble == nullableDouble

  b = nullableFloat == float
  b = float == nullableFloat
  b = float == float
  b = nullableFloat == nullableFloat

  // These are regular equals semantics
  b = double == null
  b = nullableDouble == null
  b = null == double
  b = null == nullableDouble
  b = float == null
  b = nullableFloat == null
  b = null == float
  b = null == nullableFloat

  val o1: Any? = Any()
  val o2: Any? = "Hello"
  b = o1 == o2
}

fun testTripleEquals() {
  val nullableDouble: Double? = 1.0
  val double = 1.0
  val nullableFloat: Float? = 1.0f
  val float = 1.0f

  var b = nullableDouble === double
  b = double === nullableDouble
  b = double === double
  b = nullableDouble === nullableDouble

  b = nullableFloat === float
  b = float === nullableFloat
  b = float === float
  b = nullableFloat === nullableFloat

  b = double === null
  b = nullableDouble === null
  b = null === double
  b = null === nullableDouble
  b = float === null
  b = nullableFloat === null
  b = null === float
  b = null === nullableFloat

  val o1: Any? = Any()
  val o2: Any? = "Hello"
  b = o1 === o2
}
