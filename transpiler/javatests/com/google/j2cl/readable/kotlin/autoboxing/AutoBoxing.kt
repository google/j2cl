/*
 * Copyright 2026 Google Inc.
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
package autoboxing

import javaemul.internal.annotations.DoNotAutobox
import jsinterop.annotations.JsMethod

// In Kotlin, nullable primitive types (e.g. `Int?`) are boxed and non-nullable primitive types
// (e.g. `Int`) are unboxed.
class AutoBoxing {
  fun box(b: Boolean): Boolean? = b // auto-boxing by return

  fun box(d: Double): Double? = d // auto-boxing by return

  fun box(b: Byte): Byte? = b // auto-boxing by return

  fun box(f: Float): Float? = f // auto-boxing by return

  fun box(i: Int): Int? = i // auto-boxing by return

  fun box(l: Long): Long? = l // auto-boxing by return

  fun box(s: Short): Short? = s // auto-boxing by return

  fun box(c: Char): Char? = c // auto-boxing by return

  fun unbox(b: Boolean?): Boolean = b!! // auto-unboxing by return

  fun unbox(d: Double?): Double = d!! // auto-unboxing by return

  fun unbox(b: Byte?): Byte = b!! // auto-unboxing by return

  fun unbox(f: Float?): Float = f!! // auto-unboxing by return

  fun unbox(i: Int?): Int = i!! // auto-unboxing by return

  fun unbox(l: Long?): Long = l!! // auto-unboxing by return

  fun unbox(s: Short?): Short = s!! // auto-unboxing by return

  fun unbox(c: Char?): Char = c!! // auto-unboxing by return

  fun takesAndReturnsPrimitiveDouble(d: Double): Double = d

  fun takesAndReturnsVoid(v: Void?): Void? = null

  // Nullable element type makes the varargs array an array of boxed values.
  fun takesFloatVarArgs(vararg elements: Float?) {}

  fun takesObjectAndReturnsPrimitiveDouble(@DoNotAutobox o: Any?): Double = o as Double

  fun sumWithoutBoxing(@DoNotAutobox vararg numbers: Any?): Double {
    var sum = 0.0
    for (number in numbers) {
      sum += number as Double
    }
    return sum
  }

  @JsMethod
  fun sumWithoutBoxingJsVarargs(@DoNotAutobox vararg numbers: Any?): Double {
    var sum = 0.0
    for (number in numbers) {
      sum += number as Double
    }
    return sum
  }

  fun sumWithUnboxing(boxedDoubles: Iterable<Double>): Double {
    var sum = 0.0
    for (d: Double in boxedDoubles) {
      sum += d
    }
    return sum
  }

  fun testBoxing() {
    var bool = true
    var d = 2.2
    var b: Byte = 1
    var f = 1.1f
    var i = 1
    var l = 2L
    var s: Short = 1
    var c = 'a'

    // auto-boxing by assignment
    var boxBool: Boolean? = bool
    var boxD: Double? = d
    var boxB: Byte? = b
    var boxF: Float? = f
    var boxI: Int? = i
    var boxL: Long? = l
    var boxS: Short? = s
    var boxC: Char? = c

    // auto-boxing by assignment with literals
    boxBool = true
    boxD = 2.2
    boxB = 1
    boxF = 1.1f
    boxI = 1
    boxL = 2L
    boxS = 1
    // Integer literals are not assignable to Char in Kotlin.
    // boxC = 1
    boxC = 'a'

    // auto-boxing by assignment with constant expressions
    val localConstant = 66
    // Int constants are not assignable to Char in Kotlin.
    // boxC = INT_CONSTANT
    // boxC = localConstant
    boxC = 'A' + 3
    // Kotlin does not accept constants of a narrower type either.
    // boxC = 65.toByte()

    // auto-boxing by parameter
    bool = unbox(bool)
    d = unbox(d)
    b = unbox(b)
    f = unbox(f)
    i = unbox(i)
    l = unbox(l)
    s = unbox(s)
    c = unbox(c)
    var unusedDouble = takesObjectAndReturnsPrimitiveDouble(4)
    unusedDouble = sumWithoutBoxing(1, 2.2, 1.toByte(), 1.toShort(), 2.2.toFloat())
    unusedDouble = sumWithoutBoxingJsVarargs(1, 2.2, 1.toByte(), 1.toShort(), 2.2.toFloat())
    // @DoNotAutobox doesn't apply to array creation elements.
    unusedDouble =
      sumWithoutBoxing(*arrayOf<Any?>(1, 2.2, 1.toByte(), 1.toShort(), 2.2.toFloat()))
    takesFloatVarArgs(1.1f, 'a'.code.toFloat(), 2.2.toFloat())

    // auto-boxing by assignment to Any
    var o: Any?
    o = 2.2
    o = 1.1f
    o = 1
    o = 'a'

    // auto-boxing by assignment
    boxBool = boxBool && boxBool
    boxD = boxD + boxD
    boxI = boxI / boxI
    boxL = boxL / boxL
    boxBool = !boxBool
    boxI = +boxI
    boxI = -boxI

    boxI = boxI shl boxI
    boxI = boxI shl boxL.toInt()
    boxL = boxL shl boxI
    boxL = boxL shl boxL.toInt()

    // auto-boxing by intersection cast is not possible since Kotlin doesn't have intersection
    // casts.
    // o = 15 as (Int & Comparable<Int>)
  }

  fun testUnboxing() {
    var boxBool: Boolean? = java.lang.Boolean(true) as Boolean?
    var boxD: Double? = java.lang.Double(2.2) as Double?
    var boxB: Byte? = java.lang.Byte(1.toByte()) as Byte?
    var boxF: Float? = java.lang.Float(1.1f) as Float?
    var boxI: Int? = java.lang.Integer(1) as Int?
    var boxL: Long? = java.lang.Long(1L) as Long?
    var boxS: Short? = java.lang.Short(1.toShort()) as Short?
    var boxC: Char? = java.lang.Character('a') as Char?

    // auto-unboxing by assignment
    var bool: Boolean = boxBool!!
    var d: Double = boxD!!
    var b: Byte = boxB!!
    var f: Float = boxF!!
    var i: Int = boxI!!
    var l: Long = boxL!!
    var s: Short = boxS!!
    var c: Char = boxC!!

    // auto-unboxing by parameter
    boxBool = box(boxBool)
    boxD = box(boxD)
    boxB = box(boxB)
    boxF = box(boxF)
    boxI = box(boxI)
    boxL = box(boxL)
    boxS = box(boxS)
    boxC = box(boxC)

    // auto-unboxing and widening by assignment (widening is explicit in Kotlin)
    d = boxB!!.toDouble()
    d = boxF!!.toDouble()
    d = boxI!!.toDouble()
    d = boxL!!.toDouble()
    d = boxS!!.toDouble()
    d = boxC!!.code.toDouble()

    // auto-unboxing and widening by parameter (widening is explicit in Kotlin)
    takesAndReturnsPrimitiveDouble(boxB.toDouble())
    takesAndReturnsPrimitiveDouble(boxF.toDouble())
    takesAndReturnsPrimitiveDouble(boxI.toDouble())
    takesAndReturnsPrimitiveDouble(boxL.toDouble())
    takesAndReturnsPrimitiveDouble(boxS.toDouble())
    takesAndReturnsPrimitiveDouble(boxC.code.toDouble())

    val v: Void? = takesAndReturnsVoid(takesAndReturnsVoid(null))

    // auto-unboxing by operator
    bool = boxBool!! && boxBool
    d = boxD!! + boxD
    f = boxF - boxF
    i = boxI * boxI
    l = boxL / boxL
    bool = !boxBool
    i = +boxI
    i = -boxI
    i = boxI.inv()
    boxD = -boxD
    boxI = -boxI
    when (boxI) {
      else -> {}
    }

    // Compound assignments are not expressions in Kotlin.
    // i += boxI += i += boxI
    i += boxI
    boxI += i
    i += boxI

    i = i shl boxI
    i = i shl boxL.toInt()
    l = l shl boxI
    l = l shl boxL.toInt()

    val unusedInt: Int = (null as Int?)!!
  }

  fun testUnboxingBoolean() {
    var boxB1: Boolean? = java.lang.Boolean(true) as Boolean?
    var boxB2: Boolean? = java.lang.Boolean(false) as Boolean?

    var br: Boolean
    var boxr: Boolean?

    boxr = boxB1 === boxB2
    br = boxB1 === boxB2

    boxr = boxB1 !== boxB2
    br = boxB1 !== boxB2

    boxr = boxB1!! xor boxB2!!
    br = boxB1 xor boxB2

    boxr = boxB1 and boxB2
    br = boxB1 and boxB2

    boxr = boxB1 or boxB2
    br = boxB1 or boxB2

    boxr = boxB1 && boxB2
    br = boxB1 && boxB2

    boxr = boxB1 || boxB2
    br = boxB1 || boxB2

    // Assignments and compound assignments are not expressions in Kotlin.
    // boxr = boxB1 = boxB2
    boxB1 = boxB2
    boxr = boxB1
    // br = boxB1 = boxB2
    boxB1 = boxB2
    br = boxB1

    // boxr = boxB1 &= boxB2
    boxB1 = boxB1 and boxB2
    boxr = boxB1
    // br = boxB1 &= boxB2
    boxB1 = boxB1 and boxB2
    br = boxB1

    // boxr = boxB1 |= boxB2
    boxB1 = boxB1 or boxB2
    boxr = boxB1
    // br = boxB1 |= boxB2
    boxB1 = boxB1 or boxB2
    br = boxB1

    // boxr = boxB1 ^= boxB2
    boxB1 = boxB1 xor boxB2
    boxr = boxB1
    // br = boxB1 ^= boxB2
    boxB1 = boxB1 xor boxB2
    br = boxB1

    var boxB: Boolean? = null
    boxB = !boxB!!
    boxB = boxB && boxB
    boxB = if (boxB) boxB else boxB
    if (boxB) {}
    var b = false
    // b |= boxB |= b |= boxB
    b = b or boxB
    boxB = boxB or b
    b = b or boxB
  }

  fun testUnboxingEquality() {
    val boxB: Boolean? = java.lang.Boolean(true) as Boolean?
    val b = false

    var result = boxB === boxB
    result = boxB == b

    result = b != b
    result = b != boxB

    val boxI: Int? = java.lang.Integer(1) as Int?
    val i = 1

    result = boxI === boxI
    result = boxI == i

    result = i != i
    result = i != boxI
  }

  private fun acceptsObject(x: Any) {}

  private fun acceptsInt(x: Int) {}

  private fun acceptsString(x: String) {}

  private class Ref<T>(var field: T)

  // Type arguments are always boxed, even when they are not nullable.
  private fun testUnboxingGeneric(
    integerRef: Ref<Int>,
    booleanRef: Ref<Boolean>,
    stringRef: Ref<String>,
  ) {
    integerRef.field = integerRef.field
    integerRef.field++
    integerRef.field += 1
    acceptsInt(integerRef.field)

    acceptsObject(-integerRef.field)
    acceptsObject(!booleanRef.field)
    acceptsObject(1 + integerRef.field)
    acceptsObject(booleanRef.field || booleanRef.field)
    acceptsObject(stringRef.field + stringRef.field)

    stringRef.field = stringRef.field
    acceptsString("" + integerRef.field)
  }

  fun testAssertStatement() {
    val boxB: Boolean? = java.lang.Boolean(true) as Boolean?
    val b = true

    assert(boxB!!)
    assert(b)
  }

  companion object {
    const val COMPILE_TIME_CONSTANT = 1.1f

    private const val INT_CONSTANT = 65

    // Values of type variables are always boxed, even when they are not nullable.
    @Suppress("UNCHECKED_CAST")
    fun <T : Long> testUnboxingFromTypeVariable() {
      var n = 10L as T
      // auto-unboxing from variable n.
      var l: Long = n

      // Increments on type variables don't compile in Kotlin since `inc()` returns `Long`, not `T`.
      // n++
      // ++n
      // n = n++
      // n = ++n
      n = (n + 1L) as T

      class Local<T : Long> {
        fun toLong(l: T): Long {
          // auto-unboxing from variable l.
          return l
        }
      }
      l = Local<Long>().toLong(11L)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> testUnboxingFromIntersectionType() where T : Long, T : Comparable<Long> {
      var n = 10L as T
      // auto-unboxing from variable n.
      var l: Long = n

      // Increments on type variables don't compile in Kotlin since `inc()` returns `Long`, not `T`.
      // n++
      // ++n
      // n = n++
      // n = ++n
      n = (n + 1L) as T

      class Local<T> where T : Long, T : Comparable<Long> {
        fun toLong(l: T): Long {
          // auto-unboxing from variable l.
          return l
        }
      }
      // auto-boxing parameter.
      l = Local<Long>().toLong(11L)

      // Kotlin doesn't have intersection casts.
      // val i: Int = 10 as (Int & Comparable<Int>)
    }
  }

  fun testUnbox_withCast() {
    class Supplier<T> {
      fun get(): T? = null
    }
    val supplier = Supplier<Int>()
    val i = supplier.get() as Int
  }

  fun testBoxingUnboxedValues(
    boxedInteger: Int?,
    boxedDouble: Double?,
    boxedBoolean: Boolean?,
    boxedCharacter: Char?,
    number: Number,
  ) {
    // Values that are unboxed just to be boxed again.
    val i: Int? = boxedInteger!!
    val o: Any = boxedInteger!!
    acceptsObject(boxedInteger!!)
    val d: Double? = boxedDouble!!
    val b: Boolean? = boxedBoolean!!
    val c: Char? = boxedCharacter!!
    // Unboxing to a different primitive type or from a supertype.
    val l: Long? = boxedInteger!!.toLong()
    val fromNumber: Int? = number.toInt()
  }
}
