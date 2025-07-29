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
package autoboxing

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.Asserts.fail
import com.google.j2cl.integration.testing.TestUtils.getUndefined
import com.google.j2cl.integration.testing.TestUtils.isJvm
import javaemul.internal.annotations.DoNotAutobox
import jsinterop.annotations.JsMethod

fun main(vararg unused: String) {
  testBox_byParameter()
  testBox_numberAsDouble()
  testBox_byAssignment()
  testBox_byCompoundAssignment()
  testUnbox_byParameter()
  testUnbox_byAssignment()
  testUnbox_byOperator()
  testUnbox_fromTypeVariable<Long>()
  testUnbox_fromIntersectionType<Long>()
  testUnbox_conditionals()
  testUnbox_switchExpression()
  testAutoboxing_arithmetic()
  testAutoboxing_range()
  testAutoboxing_equals()
  testAutoboxing_ternary()
  testAutoboxing_casts()
  testAutoboxing_nullCheck()
  testAutoboxing_smartCast()
  testAutoboxing_arrayExpressions()
  // Compound assignments in kotlin are not expressions.
  // testAutoboxing_compoundAssignmentSequence()
  testSideEffects_nestedIncrement()
  testSideEffects_compoundAssignment()
  testSideEffects_incrementDecrement()
  testNoBoxing_null()
}

private fun testBox_byParameter() {
  val b = 100.toByte()
  val d = 1111.0
  val f = 1111.0f
  val i = 1111
  val l = 1111L
  val s = 100.toShort()
  val bool = true
  val c = 'a'
  assertTrue(unbox(b) == b)
  assertTrue(unbox(d) == d)
  assertTrue(unbox(f) == f)
  assertTrue(unbox(i) == i)
  assertTrue(unbox(l) == l)
  assertTrue(unbox(s) == s)
  assertTrue(unbox(bool) == bool)
  assertTrue(unbox(c) == c)
  assertTrue(boxToObject(100.toByte()) is Byte)
  assertTrue(boxToObject(1111.0) is Double)
  assertTrue(boxToObject(1111.0f) is Float)
  assertTrue(boxToObject(1111) is Int)
  assertTrue(boxToObject(1111L) is Long)
  assertTrue(boxToObject(100.toShort()) is Short)
  assertTrue(boxToObject(true) is Boolean)
  assertTrue(boxToObject('a') is Char)
}

private fun testBox_numberAsDouble() {
  // Works only in closure.
  if (isJvm()) {
    return
  }
  assertTrue(takesObjectAndReturnsPrimitiveDouble(3) == 3.0)
  assertTrue(sumWithoutBoxing(1, 1.5, 1.toByte(), 1.toShort(), 1f) == 5.5)
  assertTrue(sumWithoutBoxingJsVarargs(1, 1.5, 1.toByte(), 1.toShort(), 1f) == 5.5)
}

private fun takesObjectAndReturnsPrimitiveDouble(@DoNotAutobox o: Any?): Double {
  return o as Double
}

private fun sumWithoutBoxing(@DoNotAutobox vararg numbers: Any?): Double {
  var sum = 0.0
  for (number in numbers) {
    sum += number as Double
  }
  return sum
}

// JsMethods have different varargs semantics.
@JsMethod
private fun sumWithoutBoxingJsVarargs(@DoNotAutobox vararg numbers: Any?): Double {
  var sum = 0.0
  for (number in numbers) {
    sum += number as Double
  }
  return sum
}

private fun unbox(b: Byte?): Byte {
  return b as Byte
}

private fun unbox(d: Double?): Double {
  return d as Double
}

private fun unbox(f: Float?): Float {
  return f as Float
}

private fun unbox(i: Int?): Int {
  return i as Int
}

private fun unbox(l: Long?): Long {
  return l as Long
}

private fun unbox(s: Short?): Short {
  return s as Short
}

private fun unbox(b: Boolean?): Boolean {
  return b as Boolean
}

private fun unbox(c: Char?): Char {
  return c as Char
}

private fun boxToObject(o: Any?): Any? {
  return o
}

private fun testBox_byAssignment() {
  val b = 100.toByte()
  val d = 1111.0
  val f = 1111.0f
  val i = 1111
  val l = 1111L
  val s = 100.toShort()
  val bool = true
  val c: Char = 'a'
  val boxB: Byte? = b
  val boxD: Double? = d
  val boxF: Float? = f
  val boxI: Int? = i
  val boxL: Long? = l
  val boxS: Short? = s
  val boxBool: Boolean? = bool
  val boxC: Char? = c
  assertTrue(boxB == b)
  assertTrue(boxD == d)
  assertTrue(boxF == f)
  assertTrue(boxI == i)
  assertTrue(boxL == l)
  assertTrue(boxS == s)
  assertTrue(boxBool == bool)
  assertTrue(boxC == c)

  var o: Any? = 100.toByte()
  assertTrue(o is Byte)
  o = 1111.0
  assertTrue(o is Double)
  o = 1111.0f
  assertTrue(o is Float)
  o = 1111
  assertTrue(o is Int)
  o = 1111L
  assertTrue(o is Long)
  o = 100.toShort()
  assertTrue(o is Short)
  o = true
  assertTrue(o is Boolean)
  o = 'a'
  assertTrue(o is Char)
}

// A generic holder to make sure that the values stored in the field are boxed. When the type
// variable T is specialized to a non-nullable primitive type like Int, implicit unboxing
// operations will happen at assignment, parameter passing, etc, similar to Java. There is no
// direct way to have operations that do implicit unboxing since Int? is not assignable to Int and
// requires explicit conversion.
private class Ref<T>(var field: T)

private fun testBox_byCompoundAssignment() {
  // Compound assignments can only be expression on non-nullable primitive types, to be able to
  // apply them to a boxed value we use the field in Ref<Int>() which is of a reference type.
  var i = Ref(10)
  // TODO(b/235427449): Uncomment when fixed. The lowering pass that handles these operators
  // rewrite i.field++ to (val pre : Int = i.field, i.field = pre +1, pre) whose value is
  // a primitve int.
  // assertIsBoxedInteger(i.field++)
  i.field++

  assertIsBoxedInteger(--i.field)
  assertTrue((i.field++).javaClass == Int::class.java)
  assertTrue(--i.field == 10)
}

private fun testUnbox_byParameter() {
  val boxB = Ref(100.toByte())
  val boxD = Ref(1111.0)
  val boxDNaN = Ref(0.0 / 0.0)
  val boxF = Ref(1111.0f)
  val boxFNaN = Ref(0.0f / 0.0f)
  val boxI = Ref(1111)
  val boxL = Ref(1111L)
  val boxS = Ref(100.toShort())
  val boxBool = Ref(true)
  val boxC = Ref('c')

  // Unbox
  assertTrue(box(boxB.field) == boxB.field)
  assertTrue(box(boxD.field) == boxD.field)
  assertFalse(box(boxDNaN.field) == boxDNaN.field)
  assertTrue(box(boxF.field) == boxF.field)
  assertFalse(box(boxFNaN.field) == boxFNaN.field)
  assertTrue(box(boxI.field) == boxI.field)
  assertTrue(box(boxL.field) == boxL.field)
  assertTrue(box(boxS.field) == boxS.field)
  assertTrue(box(boxBool.field) == boxBool.field)
  assertTrue(box(boxC.field) == boxC.field)
}

private fun box(b: Byte): Byte? {
  return b
}

private fun box(d: Double): Double? {
  return d
}

private fun box(f: Float): Float? {
  return f
}

private fun box(i: Int): Int? {
  return i
}

private fun box(l: Long): Long? {
  return l
}

private fun box(s: Short): Short? {
  return s
}

private fun box(b: Boolean): Boolean? {
  return b
}

private fun box(a: Char): Char? {
  return a
}

// Implicit unboxing only happens in Kotlin when a general type is specialized to a non-nullable
// primitive type.
private fun testUnbox_byAssignment() {
  val boxB = Ref(100.toByte())
  val boxD = Ref(1111.0)
  val boxF = Ref(1111.0f)
  val boxI = Ref(1111)
  val boxL = Ref(1111L)
  val boxS = Ref(100.toShort())
  val boxBool = Ref(true)
  val boxC = Ref('c')

  // Unbox
  var b: Byte = boxB.field
  var d: Double = boxD.field
  val f: Float = boxF.field
  val i: Int = boxI.field
  val l: Long = boxL.field
  val s: Short = boxS.field
  val bool: Boolean = boxBool.field
  val c: Char = boxC.field
  assertTrue(b == boxB.field)
  assertTrue(d == boxD.field)
  assertTrue(f == boxF.field)
  assertTrue(i == boxI.field)
  assertTrue(l == boxL.field)
  assertTrue(s == boxS.field)
  assertTrue(bool == boxBool.field)
  assertTrue(c == boxC.field)
}

@SuppressWarnings("SelfAssignment")
private fun testUnbox_byOperator() {
  // non side effect prefix operations
  var i = Ref(1111)
  i.field = +i.field
  assertTrue(i.field == 1111)
  i.field = -i.field
  assertTrue(i.field == -1111)
  i.field = i.field.inv()
  assertTrue(i.field == 1110)
  var bool = Ref(true)
  bool.field = !bool.field
  assertTrue(!bool.field)

  // non side effect binary operations
  val i1 = Ref(100)
  val i2 = Ref(200)
  val i3 = Ref(4)
  val sumI = i1.field + i2.field
  val boxSumI: Int? = i1.field + i2.field
  boxSumI as Int // make sure that you can express arithmetic operations in boxSumI which is boxed
  assertIsBoxedInteger(boxSumI)
  assertTrue(sumI == 300)
  assertTrue(boxSumI == 300)
  assertTrue(boxSumI != 0)
  assertTrue(boxSumI < 400)
  assertTrue(boxSumI > 200)
  assertTrue(boxSumI <= 300)
  assertTrue(boxSumI >= 300)
  val shiftedI = i2.field shl i3.field
  assertTrue(shiftedI == 3200)

  val l1 = Ref(1000L)
  val l2 = Ref(2000L)
  val sumL = l1.field + l2.field
  val boxSumL: Long? = l1.field + l2.field
  boxSumL as Long // make sure that you can express arithmetic operations in boxSumL which is boxed
  assertIsBoxedLong(boxSumL)
  assertTrue(sumL == 3000L)
  assertTrue(boxSumL == sumL)

  val d1 = Ref(1111.1)
  val d2 = Ref(2222.2)
  val sumD = d1.field + d2.field
  val boxSumD: Double? = d1.field + d2.field
  assertTrue(boxSumD == sumD)

  val b1 = Ref(true)
  val b2 = Ref(false)
  val boxB: Boolean? = b1.field && b2.field
  val b3 = b1.field || b2.field
  boxB as Boolean
  assertTrue(!boxB)
  assertTrue(b3)

  // Should not throw since it should be converted into a string using String.valueOf(Object) and
  // thus does not require an erasure casts (the JLS requires just enough erasure casts to
  // make the program type safe).
  val booleanInIntegerRef = Ref(true) as Ref<Int>
  val unusedS = "" + booleanInIntegerRef.field
}

@SuppressWarnings("SelfAssignment")
private fun testUnbox_byOperator_throwsNPE() {
  // Unboxing can cause NPE.
  val b = Ref(null) as Ref<Boolean>
  assertThrowsNullPointerException {
    val unused: Any = b.field && b.field
  }
  assertThrowsNullPointerException {
    val unused: Any = !b.field
  }
  val d = Ref(null) as Ref<Double>

  assertThrowsNullPointerException {
    val unused: Any = +d.field
  }
  val n = Ref(null) as Ref<Int>
  assertThrowsNullPointerException {
    val unused: Any = -n.field
  }
}

@SuppressWarnings("SelfAssignment")
private fun testUnbox_byOperator_throwsCCE() {
  val shortInIntegerRef = Ref(1.toShort()) as Ref<Int>
  val booleanInIntegerRef = Ref(true) as Ref<Int>
  val integerInBooleanRef = Ref(1) as Ref<Boolean>
  val integerInStringRef = Ref(1) as Ref<String>

  // TODO(b/421337782): Remove this workaround once the bug is fixed in Kotlin/JVM.
  val expectedNumberClass = if (isJvm()) Number::class.java else Int::class.javaObjectType
  // Unboxing can cause ClassCastException.
  assertThrowsClassCastException({ booleanInIntegerRef.field++ }, expectedNumberClass)

  assertThrowsClassCastException(
    {
      val unused: Any = -booleanInIntegerRef.field
    },
    expectedNumberClass,
  )

  assertThrowsClassCastException(
    {
      val unused: Any = !integerInBooleanRef.field
    },
    Boolean::class.javaObjectType,
  )

  assertThrowsClassCastException({ booleanInIntegerRef.field += 1 }, expectedNumberClass)

  assertThrowsClassCastException(
    {
      val unused: Any = 1 + booleanInIntegerRef.field
    },
    expectedNumberClass,
  )

  assertThrowsClassCastException(
    {
      val unused: Any = integerInBooleanRef.field || integerInBooleanRef.field
    },
    Boolean::class.javaObjectType,
  )

  // TODO(b/254148464): J2CL is more strict about erasure casts than JVM. Here JVM does not throw
  // assertThrows while J2CL does.
  if (!isJvm()) {
    assertThrowsClassCastException(
      {
        val unused: Any = integerInStringRef.field + integerInStringRef.field
      },
      String::class.javaObjectType,
    )
  }

  // TODO(b/254148464): JVM does not throw assertThrows while J2CL does.
  if (!isJvm()) {
    assertThrowsClassCastException(
      { integerInStringRef.field = integerInStringRef.field },
      String::class.java,
    )
  }

  // TODO(b/254148464): JVM does not throw assertThrows while J2CL does.
  if (!isJvm()) {
    assertThrowsClassCastException(
      {
        val unused = shortInIntegerRef.field
      },
      Int::class.javaObjectType,
    )
  }

  // TODO(b/254148464): JVM does not throw assertThrows while J2CL does.
  if (!isJvm()) {
    assertThrowsClassCastException(
      { acceptsInt(shortInIntegerRef.field) },
      Int::class.javaObjectType,
    )
  }
}

private fun acceptsInt(x: Int) {}

private const val foo = 0

private fun testNoBoxing_null() {
  // Avoiding a "condition always evaluates to true" error in JSComp type checking.
  val maybeNull = if (foo == 0) null else Any()
  val bool: Boolean? = null
  val d: Double? = null
  val i: Int? = null
  val l: Long? = null
  assertTrue(bool === maybeNull)
  assertTrue(d === maybeNull)
  assertTrue(i === maybeNull)
  assertTrue(l === maybeNull)
}

private fun testAutoboxing_arithmetic() {
  var b = Ref(0.toByte())
  var c = Ref('\u0000')
  var s = Ref(6000.toShort())
  var i = Ref(6000)
  var l = Ref(6000L)
  var f = Ref(6000.0f)
  var d = Ref(6000.0)
  val nullInt: Int? = null
  b.field++
  c.field++
  s.field++
  i.field++
  l.field++
  f.field++
  d.field++
  assertTrue(b.field == 1.toByte())
  assertTrue(c.field == '\u0001')
  assertTrue(s.field == 6001.toShort())
  assertTrue(i.field == 6001)
  assertTrue(l.field == 6001L)
  assertTrue(f.field == 6001.0f)
  assertTrue(d.field == 6001.0)
  assertTrue(i.field != nullInt)
  assertTrue(nullInt != i.field)

  for (j in 0..199) {
    b.field++
  }
  assertTrue(b.field == (-55).toByte())
}

private fun testAutoboxing_range() {
  val from: Short? = 1.toShort()
  val to: Long? = 5L
  var r = 0L
  if (from is Short && to is Long) {
    for (i in from..to) {
      r += i
    }
  }
  assertEquals(15L, r)
}

private fun testAutoboxing_equals() {
  val zero = 0.0
  val minusZero = -0.0
  val boxedZero: Double? = 0.0
  val boxedMinusZero: Double? = -0.0
  val asObjectZero: Any = zero
  val asObjectMinusZero: Any = minusZero
  val nullDouble: Double? = null
  val undefinedDouble: Double? = getUndefined()

  // Unboxing semantics.
  assertTrue(zero === minusZero)
  assertTrue(minusZero === zero)
  assertFalse(zero === boxedMinusZero)
  assertFalse(minusZero === nullDouble)
  assertFalse(minusZero === undefinedDouble)

  // Reference semantics.
  assertFalse(boxedZero === minusZero) // boxing driven by the lhs.
  assertFalse(nullDouble === zero) // boxing driven by the lhs.

  assertFalse(boxedZero === boxedMinusZero)
  assertFalse(asObjectZero === asObjectMinusZero)
  assertTrue(undefinedDouble === nullDouble)

  // Explicit unboxing.
  // TODO(b/237539338): enable when bug is fixed.
  // assertTrue(asObjectZero as Double === asObjectMinusZero as Double)

  // Unboxing semantics
  assertTrue(zero == minusZero)
  assertTrue(minusZero == zero)
  assertTrue(zero == boxedMinusZero)

  // Special semantics through (ieee754equals)
  assertTrue(boxedZero == boxedMinusZero)
  assertTrue(boxedZero == minusZero)
  assertFalse(nullDouble == zero)
  assertFalse(zero == nullDouble)
  assertFalse(undefinedDouble == zero)
  assertFalse(zero == undefinedDouble)
  assertTrue(undefinedDouble == nullDouble)

  // Boxed .equals() semantics.
  assertFalse(asObjectZero == asObjectMinusZero)

  // Explicit unboxing.
  assertTrue(asObjectZero as Double == asObjectMinusZero as Double)

  val int5000 = 5000
  val boxedInt5000: Int? = int5000
  val nullInt: Int? = null

  assertTrue(int5000 == boxedInt5000)
  assertTrue(boxedInt5000 == int5000)
  assertTrue(nullInt == nullInt)
  assertFalse(nullInt == int5000)
  assertFalse(int5000 == nullInt)
}

var TRUE: Boolean = false

private fun testAutoboxing_ternary() {
  val boxedValue: Int? = 1
  val primitiveValue = 10

  var boxedResult: Int?
  var primitiveResult: Int

  // Just to avoid JSCompiler being unhappy about "suspicious code" when seeing a ternary that
  // always evaluates to true.
  TRUE = foo == 0

  boxedResult = if (TRUE) boxedValue else boxedValue
  assertTrue(boxedResult == 1)

  boxedResult = if (TRUE) boxedValue else primitiveValue
  assertTrue(boxedResult == 1)

  boxedResult = if (TRUE) primitiveValue else boxedValue
  assertTrue(boxedResult == 10)

  boxedResult = if (TRUE) primitiveValue else primitiveValue
  assertTrue(boxedResult == 10)

  boxedValue as Int // force the compiler to know it can unbox the boxes below.
  primitiveResult = if (TRUE) boxedValue else boxedValue
  assertTrue(primitiveResult == 1)

  primitiveResult = if (TRUE) boxedValue else primitiveValue
  assertTrue(primitiveResult == 1)

  primitiveResult = if (TRUE) primitiveValue else boxedValue
  assertTrue(primitiveResult == 10)

  primitiveResult = if (TRUE) primitiveValue else primitiveValue
  assertTrue(primitiveResult == 10)

  val b = Ref(null) as Ref<Boolean>
  assertThrowsNullPointerException {
    val unused: Any = if (b.field) b else b
  }
}

private fun testAutoboxing_casts() {
  // Box
  val boxedInteger: Int? = 100
  // Unbox by cast
  val primitiveInteger: Int = boxedInteger as Int // unbox by cast

  assertIsBoxedInteger(boxedInteger)
  assertTrue(boxedInteger is Int)
  assertTrue(primitiveInteger == 100)

  val implicitUnboxing: Int = boxedInteger
  assertIsPrimitiveInteger(implicitUnboxing)
}

private fun testAutoboxing_nullCheck() {
  // Box
  val boxedInteger: Int? = 100
  // Unbox by null check
  val primitiveInteger: Int = boxedInteger!!

  assertIsBoxedInteger(boxedInteger)
  assertTrue(boxedInteger is Int)
  assertTrue(primitiveInteger == 100)

  val implicitUnboxing: Int = boxedInteger
  assertIsPrimitiveInteger(implicitUnboxing)
}

private fun testAutoboxing_smartCast() {
  // Box
  val boxedInteger: Int? = 100

  if (boxedInteger is Int) {
    // Smart cast makes the assignment to implicitly unbox.
    val primitiveInteger: Int = boxedInteger

    assertIsBoxedInteger(boxedInteger)
    assertTrue(boxedInteger is Int)
    assertTrue(primitiveInteger == 100)

    val implicitUnboxing: Int = boxedInteger
    assertIsPrimitiveInteger(implicitUnboxing)
  }
}

private fun testAutoboxing_arrayExpressions() {
  val boxedInteger1 = Ref(100)
  val boxedInteger2 = Ref(50)
  val objects = arrayOfNulls<Any>(boxedInteger1.field)
  assertTrue(objects.size == 100)
  val marker = Any()
  objects[boxedInteger2.field] = marker
  assertTrue(objects[50] === marker)

  val boxedIntegers = arrayOf<Int?>(1, 2, 3)
  assertTrue(boxedIntegers[0] is Int)
  val primitiveInts: IntArray = intArrayOf(Ref(1).field, Ref(2).field, Ref(3).field)
  assertTrue(primitiveInts[0] == 1)
}

/**
 * Actually the boolean conditional unboxings don't get inserted and aren't needed because we have
 * devirtualized Boolean.
 */
private fun testUnbox_conditionals() {
  val boxedFalseBoolean = Ref(false)
  if (boxedFalseBoolean.field) {
    // If unboxing is missing we'll arrive here.
    fail()
  }
  while (boxedFalseBoolean.field) { // implicit unboxing
    // If unboxing is missing we'll arrive here.
    fail()
  }
  var count = Ref(0)
  do {
    if (count.field > 0) {
      // If unboxing is missing we'll arrive here.
      fail()
    }
    count.field++
  } while (boxedFalseBoolean.field)
  while (boxedFalseBoolean.field) {

    // If unboxing is missing we'll arrive here.
    fail()
  }
  val unusedBlah: Any? = if (boxedFalseBoolean.field) doFail() else doNothing()
  val b: Ref<Boolean> = Ref(null) as Ref<Boolean>
  assertThrowsNullPointerException { if (b.field) {} }
  assertThrowsNullPointerException { while (b.field) {} }
  assertThrowsNullPointerException { while (b.field) {} }
  assertThrowsNullPointerException { do {} while (b.field) }
  assertThrowsNullPointerException {
    var i: Ref<Int> = Ref(null) as Ref<Int>
    when (i.field) {
      1 -> i.field = 3
      else -> {}
    }
  }
}

private fun testUnbox_switchExpression() {
  val intHolder = Ref(100)
  when (intHolder.field) {
    100 -> {}
    else -> // If unboxing is missing we'll arrive here.
    fail()
  }
}

private fun testAutoboxing_compoundAssignmentSequence() {
  var boxI = Ref(1)
  var i = 2
  // Compound assignment is not an expression in Kotlin
  // boxI /* 6 */ += i /* 5 */ += boxI /* 3 */ += i /* 2*/;

  // assertTrue(i == 5);
  // assertTrue(boxI == 6);
}

private fun <T : Long> testUnbox_fromTypeVariable() {
  val n = Ref(10L as T)
  // Auto unboxing from variable n.
  var l: Long = n.field
  assertIsBoxedLong(n.field)
  assertTrue(l == 10L)
  class Local<T : Long?> {
    fun toLong(l: T): T {
      // Auto unboxing from variable l.
      assertTrue(l == 11L)
      return l
    }
  }

  // Auto boxing parameter.
  l = Local<Long>().toLong(11L)
  assertTrue(l == 11L)
}

private fun <T> testUnbox_fromIntersectionType() where T : Long, T : Comparable<Long> {
  val n: T = 10L as T
  // Auto unboxing from variable n.
  var l: Long = n
  assertIsBoxedLong(n)
  assertTrue(l == 10L)
  class Local<T> where T : Long, T : Comparable<Long>? {
    fun toLong(l: T): Long {
      // Auto unboxing from variable l.
      assertTrue(l == 11L)
      return l
    }
  }

  // Auto boxing parameter.
  l = Local<Long>().toLong(11L)
  assertTrue(l == 11L)
}

private class SideEffectTester {
  inner class RefHolder<T>(contents: T) {
    var contents: T = contents
      get() = field
      set(value) {
        sideEffectCount++
        field = value
      }
  }

  val doubleFieldHolder = RefHolder(1111.1)
  val integerFieldHolder = RefHolder(1111)
  val longFieldHolder = RefHolder(1111L)
  var sideEffectCount = 0

  fun fluentAssertEquals(expectedValue: Any, testValue: Any): SideEffectTester {
    assertEquals(expectedValue, testValue)
    return this
  }
}

/** Test increment and decrement operators on boxing values. */
private fun testSideEffects_incrementDecrement() {
  val tester = SideEffectTester()
  assertTrue(tester.sideEffectCount == 0)
  assertEquals(1111, tester.integerFieldHolder.contents++)
  assertTrue(tester.integerFieldHolder.contents == 1112)
  assertTrue(tester.sideEffectCount == 1)
  assertEquals(1111L, tester.longFieldHolder.contents++)
  assertTrue(tester.longFieldHolder.contents == 1112L)
  assertTrue(tester.sideEffectCount == 2)
  assertEquals(1112, tester.integerFieldHolder.contents--)
  assertTrue(tester.integerFieldHolder.contents == 1111)
  assertTrue(tester.sideEffectCount == 3)
  assertEquals(1112L, tester.longFieldHolder.contents--)
  assertTrue(tester.longFieldHolder.contents == 1111L)
  assertTrue(tester.sideEffectCount == 4)
  assertTrue(1112 == ++tester.integerFieldHolder.contents)
  assertTrue(tester.integerFieldHolder.contents == 1112)
  assertTrue(tester.sideEffectCount == 5)
  assertTrue(1112L == ++tester.longFieldHolder.contents)
  assertTrue(tester.longFieldHolder.contents == 1112L)
  assertTrue(tester.sideEffectCount == 6)
  assertTrue(1111 == --tester.integerFieldHolder.contents)
  assertTrue(tester.integerFieldHolder.contents == 1111)
  assertTrue(tester.sideEffectCount == 7)
  assertTrue(1111L == --tester.longFieldHolder.contents)
  assertTrue(tester.longFieldHolder.contents == 1111L)
  assertTrue(tester.sideEffectCount == 8)
  assertEquals(1111.1, tester.doubleFieldHolder.contents++)
  assertTrue(tester.doubleFieldHolder.contents == 1112.1)
  assertEquals(1112.1, tester.doubleFieldHolder.contents--)
  assertTrue(tester.doubleFieldHolder.contents == 1111.1)
  assertTrue(1112.1 == ++tester.doubleFieldHolder.contents)
  assertTrue(tester.doubleFieldHolder.contents == 1112.1)
  assertTrue(1111.1 == --tester.doubleFieldHolder.contents)
  assertTrue(tester.doubleFieldHolder.contents == 1111.1)
}

// /** Test compound assignment.  */
private fun testSideEffects_compoundAssignment() {
  val tester = SideEffectTester()
  tester.doubleFieldHolder.contents += 10.0
  assertTrue(tester.doubleFieldHolder.contents == 1121.1)
  assertTrue(tester.sideEffectCount == 1)
  tester.integerFieldHolder.contents += 10
  assertTrue(tester.integerFieldHolder.contents == 1121)
  assertTrue(tester.sideEffectCount == 2)
  tester.longFieldHolder.contents += 10L
  assertTrue(tester.longFieldHolder.contents == 1121L)
  assertTrue(tester.sideEffectCount == 3)
}

/** Test nested increments. */
private fun testSideEffects_nestedIncrement() {
  val tester = SideEffectTester()
  tester.fluentAssertEquals(
    1113L,
    tester
      .fluentAssertEquals(
        1112L,
        tester
          .fluentAssertEquals(1111L, tester.longFieldHolder.contents++)
          .longFieldHolder
          .contents++,
      )
      .longFieldHolder
      .contents++,
  )
  tester
    .fluentAssertEquals(1111, tester.integerFieldHolder.contents++)
    .fluentAssertEquals(1112, tester.integerFieldHolder.contents++)
    .fluentAssertEquals(1113, tester.integerFieldHolder.contents++)
  tester
    .fluentAssertEquals(
      1113.1,
      tester
        .fluentAssertEquals(1111.1, tester.doubleFieldHolder.contents++)
        .fluentAssertEquals(1112.1, tester.doubleFieldHolder.contents++)
        .doubleFieldHolder
        .contents++,
    )
    .fluentAssertEquals(
      1116.1,
      tester
        .fluentAssertEquals(1114.1, tester.doubleFieldHolder.contents++)
        .fluentAssertEquals(1115.1, tester.doubleFieldHolder.contents++)
        .doubleFieldHolder
        .contents++,
    )
}

private fun doFail(): Any? {
  fail()
  return null
}

private fun doNothing(): Any? {
  return null
}

private fun assertIsBoxedLong(@DoNotAutobox obj: Any?) {
  assertTrue(obj is Long)
}

private fun assertIsBoxedInteger(@DoNotAutobox obj: Any?) {
  assertTrue(obj is Int)
}

private fun assertIsPrimitiveInteger(@DoNotAutobox obj: Any) {
  // There is no way to observe if a value is a primitive or reference type when running in the JVM
  if (isJvm()) {
    return
  }
  assertTrue(obj is Double)
}
