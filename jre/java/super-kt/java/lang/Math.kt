/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package java.lang

import kotlin.Boolean
import kotlin.math.IEEErem
import kotlin.math.nextTowards
import kotlin.math.nextUp
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.ulp
import kotlin.math.withSign

/**
 * Implementation of java.lang.Math for Kotlin Native.
 *
 * This is only meant to be used in generated code. See regular JRE documentation for javadoc.
 */
object Math {
  const val E: Double = kotlin.math.E

  const val PI: Double = kotlin.math.PI

  private const val PI_OVER_180 = PI / 180.0

  private const val PI_UNDER_180 = 180.0 / PI

  fun abs(x: Double): Double = kotlin.math.abs(x)

  fun abs(x: Float): Float = kotlin.math.abs(x)

  fun abs(x: Int): Int = kotlin.math.abs(x)

  fun abs(x: Long): Long = kotlin.math.abs(x)

  fun addExact(x: Int, y: Int): Int {
    val r = x + y
    // "Hacker's Delight" 2-12 Overflow if both arguments have the opposite sign of the result
    checkCriticalArithmetic((x xor r) and (y xor r) >= 0)
    return r
  }

  fun addExact(x: Long, y: Long): Long {
    val r = x + y
    // "Hacker's Delight" 2-12 Overflow if both arguments have the opposite sign of the result
    checkCriticalArithmetic((x xor r) and (y xor r) >= 0)
    return r
  }

  fun acos(d: Double): Double = kotlin.math.acos(d)

  fun asin(d: Double): Double = kotlin.math.asin(d)

  fun atan(d: Double): Double = kotlin.math.atan(d)

  fun atan2(y: Double, x: Double) = kotlin.math.atan2(y, x)

  fun cbrt(d: Double): Double {
    return if (d == 0.0 || !d.isFinite()) d else d.pow(1.0 / 3.0)
  }

  fun ceil(d: Double): Double = kotlin.math.ceil(d)

  fun cos(d: Double): Double = kotlin.math.cos(d)

  fun cosh(d: Double): Double = kotlin.math.cosh(d)

  fun exp(d: Double): Double = kotlin.math.exp(d)

  fun expm1(d: Double): Double = kotlin.math.expm1(d)

  fun floor(x: Double) = kotlin.math.floor(x)

  fun floorDiv(dividend: Int, divisor: Int): Int = dividend.floorDiv(divisor)

  fun floorDiv(dividend: Long, divisor: Long): Long = dividend.floorDiv(divisor)

  fun floorMod(dividend: Int, divisor: Int): Int = dividend.mod(divisor)

  fun floorMod(dividend: Long, divisor: Long): Long = dividend.mod(divisor)

  fun hypot(x: Double, y: Double): Double = kotlin.math.hypot(x, y)

  fun IEEEremainder(x: Double, y: Double): Double = x.IEEErem(y)

  fun log(d: Double): Double = kotlin.math.log(d, kotlin.math.E)

  fun log10(d: Double): Double = kotlin.math.log10(d)

  fun log1p(d: Double): Double = kotlin.math.log(d + 1.0, kotlin.math.E)

  fun max(x: Double, y: Double): Double = kotlin.math.max(x, y)

  fun max(x: Float, y: Float): Float = kotlin.math.max(x, y)

  fun max(x: Int, y: Int): Int = kotlin.math.max(x, y)

  fun max(x: Long, y: Long): Long = kotlin.math.max(x, y)

  fun min(x: Double, y: Double): Double = kotlin.math.min(x, y)

  fun min(x: Float, y: Float): Float = kotlin.math.min(x, y)

  fun min(x: Int, y: Int): Int = kotlin.math.min(x, y)

  fun min(x: Long, y: Long): Long = kotlin.math.min(x, y)

  fun multiplyExact(x: Int, y: Int): Int {
    val lr = x.toLong() * y.toLong()
    val r = lr.toInt()
    checkCriticalArithmetic(r.toLong() == lr)
    return r
  }

  fun multiplyExact(x: Long, y: Long): Long {
    if (y == -1L) {
      return negateExact(x)
    }
    if (y == 0L) {
      return 0
    }
    val r: Long = x * y
    checkCriticalArithmetic(r / y == x)
    return r
  }

  fun negateExact(x: Int): Int {
    checkCriticalArithmetic(x == Int.MIN_VALUE)
    return -x
  }

  fun negateExact(x: Long): Long {
    checkCriticalArithmetic(x == Long.MIN_VALUE)
    return -x
  }

  fun pow(x: Double, y: Double) = x.pow(y)

  fun rint(d: Double): Double = kotlin.math.round(d)

  fun round(d: Double): Long = d.roundToLong()

  fun round(f: Float): Int = f.roundToInt()

  fun signum(d: Double): Double = kotlin.math.sign(d)

  fun signum(f: Float): Float = kotlin.math.sign(f)

  fun sin(d: Double): Double = kotlin.math.sin(d)

  fun sinh(d: Double): Double = kotlin.math.sinh(d)

  fun sqrt(d: Double): Double = kotlin.math.sqrt(d)

  fun subtractExact(x: Int, y: Int): Int {
    val r = x - y
    // "Hacker's Delight" Overflow if the arguments have different signs and
    // the sign of the result is different than the sign of x
    checkCriticalArithmetic((x xor y) and (x xor r) >= 0)
    return r
  }

  fun substractExact(x: Long, y: Long): Long {
    val r = x - y
    // "Hacker's Delight" Overflow if the arguments have different signs and
    // the sign of the result is different than the sign of x
    checkCriticalArithmetic((x xor y) and (x xor r) >= 0)
    return r
  }

  fun tan(d: Double): Double = kotlin.math.tan(d)

  fun tanh(d: Double): Double = kotlin.math.tanh(d)

  fun random(): Double = kotlin.random.Random.nextDouble()

  fun toRadians(angdeg: Double): Double = angdeg * PI_OVER_180

  fun toDegrees(angrad: Double): Double = angrad * PI_UNDER_180

  fun toIntExact(x: Long): Int {
    val ix = x.toInt()
    checkCriticalArithmetic(ix.toLong() == x)
    return ix
  }

  fun ulp(d: Double): Double = d.ulp

  fun ulp(f: Float): Float = f.ulp

  fun copySign(magnitude: Double, sign: Double): Double = magnitude.withSign(sign)

  fun copySign(magnitude: Float, sign: Float): Float = magnitude.withSign(sign)

  fun getExponent(d: Double): Double = TODO("b/226319599: Implement missing Math methods.")

  fun getExponent(f: Float): Float = TODO("b/226319599: Implement missing Math methods.")

  fun nextAfter(start: Double, direction: Double): Double = start.nextTowards(direction)

  fun nextAfter(start: Float, direction: Float): Float = start.nextTowards(direction)

  fun nextUp(d: Double): Double = d.nextUp()

  fun nextUp(f: Float): Float = f.nextUp()

  fun scalb(d: Double, scaleFactor: Int): Double {
    return if (scaleFactor >= 31 || scaleFactor <= -31) {
      d * (2.0).pow(scaleFactor.toDouble())
    } else if (scaleFactor > 0) {
      d * (1 shl scaleFactor)
    } else if (scaleFactor == 0) {
      d
    } else {
      d / (1 shl -scaleFactor)
    }
  }

  fun scalb(f: Float, scaleFactor: Int): Float = scalb(f.toDouble(), scaleFactor).toFloat()

  private fun checkCriticalArithmetic(expression: Boolean) {
    if (!expression) {
      throw ArithmeticException()
    }
  }
}
