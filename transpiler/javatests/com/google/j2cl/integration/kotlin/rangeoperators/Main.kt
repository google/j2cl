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
package rangeoperators

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertNotEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.Asserts.fail

fun main(vararg unused: String) {
  testCharRangeContains()
  testCharRangeIterator()
  testIntRangeContains()
  testIntRangeIterator()
  testLongRangeContains()
  testLongRangeIterator()
  testFloatRangeContains()
  testDoubleRangeContains()
  testComparableRangeContains()
  testRangeIntrinsics()
}

fun testCharRangeContains() {
  assertTrue('a' in 'a'..'z')
  assertTrue('z' in 'a'..'z')
  assertTrue('e' in 'a'..'z')
  assertFalse('a' in '0'..'9')
  assertFalse('A' in 'a'..'z')
  assertFalse('1' in 'a'..'z')
  assertTrue('a' in 'a'..'a')

  assertTrue('a' in 'a'..<'z')
  assertFalse('z' in 'a'..<'z')
  assertTrue('e' in 'a'..<'z')
  assertFalse('a' in '0'..<'9')
  assertFalse('A' in 'a'..<'z')
  assertFalse('1' in 'a'..<'z')
  assertFalse('a' in 'a'..<'a')

  // Ranges with a greater first value are empty.
  assertFalse('a' in 'b'..'a')
  assertFalse('b' in 'b'..'a')
  assertFalse('c' in 'b'..'a')

  assertFalse('a' in 'b'..<'a')
  assertFalse('b' in 'b'..<'a')
  assertFalse('c' in 'b'..<'a')

  if ('a' in 'a'..'z') {} else {
    fail()
  }

  if ('a' in 'b'..'z') {
    fail()
  } else {}

  if ('a' in 'a'..<'z') {} else {
    fail()
  }

  if ('a' in 'b'..<'z') {
    fail()
  } else {}

  if ('z' in 'a'..<'z') {
    fail()
  } else {}

  if ('z' in 'b'..<'y') {
    fail()
  } else {}
}

fun testCharRangeIterator() {
  var s = ""
  for (c in 'a'..'d') {
    s += c
  }
  assertEquals("abcd", s)

  s = ""
  for (c in 'a'..<'d') {
    s += c
  }
  assertEquals("abc", s)
}

fun testIntRangeContains() {
  assertTrue(0 in 0..9)
  assertTrue(9 in 0..9)
  assertTrue(5 in 0..9)
  assertFalse(-1 in 0..9)
  assertFalse(10 in 0..9)
  assertFalse(100 in 0..9)
  assertTrue(-1 in -1..1)
  assertTrue(0 in -1..1)
  assertFalse(-2 in -1..1)
  assertTrue(0 in 0..0)
  // Ranges with a greater first value are empty.
  assertFalse(0 in 1..0)
  assertFalse(1 in 1..0)
  assertFalse(2 in 1..0)

  assertTrue(0 in 0..<9)
  assertFalse(9 in 0..<9)
  assertTrue(5 in 0..<9)
  assertFalse(-1 in 0..<9)
  assertFalse(10 in 0..<9)
  assertFalse(100 in 0..<9)
  assertTrue(-1 in -1..<1)
  assertTrue(0 in -1..<1)
  assertFalse(-2 in -1..<1)
  assertFalse(0 in 0..<0)
  // Ranges with a greater first value are empty.
  assertFalse(0 in 1..<0)
  assertFalse(1 in 1..<0)
  assertFalse(2 in 1..<0)
}

private var rangeFrom = 1

private fun getAndIncrementRangeFrom() = rangeFrom++

private var rangeTo = 5

private fun getAndIncrementRangeTo() = rangeTo++

fun testIntRangeIterator() {
  var r = 0
  var s = ""
  for (i in 1..5) {
    r += i
    s += i
  }
  assertEquals(15, r)
  assertEquals("12345", s)

  r = 0
  s = ""
  for (i in 1..<5) {
    r += i
    s += i
  }
  assertEquals(10, r)
  assertEquals("1234", s)

  // Ensure the operands are only evaluated once.
  rangeFrom = 1
  rangeTo = 5
  r = 0
  for (i in getAndIncrementRangeFrom()..getAndIncrementRangeTo()) {
    r += i
  }
  assertEquals(15, r)

  r = 0
  for (i in getAndIncrementRangeFrom()..<getAndIncrementRangeTo()) {
    r += i
  }
  assertEquals(14, r)

  // Ensure the operands are only evaluated once.
  rangeFrom = 1
  rangeTo = 5
  val range = getAndIncrementRangeFrom()..getAndIncrementRangeTo()
  for (k in 1..5) {
    r = 0
    for (i in range) {
      r += i
    }
    assertEquals(15, r)
  }

  val rangeUntil = getAndIncrementRangeFrom()..<getAndIncrementRangeTo()
  for (k in 1..<5) {
    r = 0
    for (i in rangeUntil) {
      r += i
    }
    assertEquals(14, r)
  }

  r = 0
  s = ""
  for (i in 1..5 step 2) {
    r += i
    s += i
  }
  assertEquals(9, r)
  assertEquals("135", s)

  r = 0
  s = ""
  for (i in 1..<5 step 2) {
    r += i
    s += i
  }
  assertEquals(4, r)
  assertEquals("13", s)

  r = 0
  s = ""
  for (i in 1 until 5) {
    r += i
    s += i
  }
  assertEquals(10, r)
  assertEquals("1234", s)

  r = 0
  s = ""
  for (i in 5 downTo 1) {
    r += i
    s += i
  }
  assertEquals(15, r)
  assertEquals("54321", s)

  var str = ""
  for (i in (1..5).reversed()) {
    str += i
  }
  assertEquals("54321", str)

  str = ""
  for (i in (1..<5).reversed()) {
    str += i
  }
  assertEquals("4321", str)
}

fun testLongRangeContains() {
  assertTrue(0L in 0L..9L)
  assertTrue(9L in 0L..9L)
  assertTrue(5L in 0L..9L)
  assertFalse(-1L in 0L..9L)
  assertFalse(10L in 0L..9L)
  assertFalse(100L in 0L..9L)
  assertTrue(-1L in -1L..1L)
  assertTrue(0L in -1L..1L)
  assertFalse(-2L in -1L..1L)
  assertTrue(0L in 0L..0L)
  // Ranges with a greater first value are empty.
  assertFalse(0L in 1L..0L)
  assertFalse(1L in 1L..0L)
  assertFalse(2L in 1L..0L)

  assertTrue(0L in 0L..<9L)
  assertFalse(9L in 0L..<9L)
  assertTrue(5L in 0L..<9L)
  assertFalse(-1L in 0L..<9L)
  assertFalse(10L in 0L..<9L)
  assertFalse(100L in 0L..<9L)
  assertTrue(-1L in -1L..<1L)
  assertTrue(0L in -1L..<1L)
  assertFalse(-2L in -1L..<1L)
  assertFalse(0L in 0L..<0L)
  // Ranges with a greater first value are empty.
  assertFalse(0L in 1L..<0L)
  assertFalse(1L in 1L..<0L)
  assertFalse(2L in 1L..<0L)
}

private var rangeFromLong = 1L

private fun getAndIncrementRangeFromLong(): Long = rangeFromLong++

private var rangeToLong = 5L

private fun getAndIncrementRangeToLong(): Long = rangeToLong++

fun testLongRangeIterator() {
  var r = 0L
  var s = ""
  for (i in 1L..5L) {
    r += i
    s += i
  }
  assertEquals(15L, r)
  assertEquals("12345", s)

  r = 0L
  s = ""
  for (i in 1L..<5L) {
    r += i
    s += i
  }
  assertEquals(10L, r)
  assertEquals("1234", s)

  r = 0L
  for (i in 1..5L) {
    r += i
  }
  assertEquals(15L, r)

  r = 0L
  for (i in 1..<5L) {
    r += i
  }
  assertEquals(10L, r)

  r = 0L
  for (i in 1L..5) {
    r += i
  }
  assertEquals(15L, r)

  r = 0L
  for (i in 1..<5) {
    r += i
  }
  assertEquals(10L, r)

  // Ensure the operands are only evaluated once.
  rangeFromLong = 1L
  rangeToLong = 5L
  r = 0L
  for (i in getAndIncrementRangeFromLong()..getAndIncrementRangeToLong()) {
    r += i
  }
  assertEquals(15L, r)

  r = 0L
  for (i in getAndIncrementRangeFromLong()..<getAndIncrementRangeToLong()) {
    r += i
  }
  assertEquals(14L, r)

  // Ensure the operands are only evaluated once.
  rangeFromLong = 1L
  rangeToLong = 5L
  val range = getAndIncrementRangeFromLong()..getAndIncrementRangeToLong()
  for (k in 1..5) {
    r = 0L
    for (i in range) {
      r += i
    }
    assertEquals(15L, r)
  }

  val rangeUntil = getAndIncrementRangeFromLong()..<getAndIncrementRangeToLong()
  for (k in 1..<5) {
    r = 0L
    for (i in rangeUntil) {
      r += i
    }
    assertEquals(14L, r)
  }

  r = 0
  s = ""
  for (i in 1L..5L step 2) {
    r += i
    s += i
  }
  assertEquals(9L, r)
  assertEquals("135", s)

  r = 0
  s = ""
  for (i in 1L..<5L step 2) {
    r += i
    s += i
  }
  assertEquals(4L, r)
  assertEquals("13", s)

  r = 0
  s = ""
  for (i in 1L until 5L) {
    r += i
    s += i
  }
  assertEquals(10L, r)
  assertEquals("1234", s)

  r = 0
  s = ""
  for (i in 5L downTo 1L) {
    r += i
    s += i
  }
  assertEquals(15L, r)
  assertEquals("54321", s)
}

fun testFloatRangeContains() {
  assertTrue(0f in 0f..9f)
  assertTrue(9f in 0f..9f)
  assertTrue(5f in 0f..9f)
  assertFalse(-1f in 0f..9f)
  assertFalse(10f in 0f..9f)
  assertFalse(100f in 0f..9f)
  assertTrue(-1f in -1f..1f)
  assertTrue(0f in -1f..1f)
  assertFalse(-2f in -1f..1f)
  assertTrue(0f in 0f..0f)
  // Ranges with a greater first value are empty.
  assertFalse(0f in 1f..0f)
  assertFalse(1f in 1f..0f)
  assertFalse(2f in 1f..0f)

  assertTrue(0f in 0f..<9f)
  assertFalse(9f in 0f..<9f)
  assertTrue(5f in 0f..<9f)
  assertFalse(-1f in 0f..<9f)
  assertFalse(10f in 0f..<9f)
  assertFalse(100f in 0f..<9f)
  assertTrue(-1f in -1f..<1f)
  assertTrue(0f in -1f..<1f)
  assertFalse(-2f in -1f..<1f)
  assertFalse(0f in 0f..<0f)
  // Ranges with a greater first value are empty.
  assertFalse(0f in 1f..<0f)
  assertFalse(1f in 1f..<0f)
  assertFalse(2f in 1f..<0f)
}

fun testDoubleRangeContains() {
  assertTrue(0.0 in 0.0..9.0)
  assertTrue(9.0 in 0.0..9.0)
  assertTrue(5.0 in 0.0..9.0)
  assertFalse(-1.0 in 0.0..9.0)
  assertFalse(10.0 in 0.0..9.0)
  assertFalse(100.0 in 0.0..9.0)
  assertTrue(-1.0 in -1.0..1.0)
  assertTrue(0.0 in -1.0..1.0)
  assertFalse(-2.0 in -1.0..1.0)
  assertTrue(0.0 in 0.0..0.0)
  // Ranges with a greater first value are empty.
  assertFalse(0.0 in 1.0..0.0)
  assertFalse(1.0 in 1.0..0.0)
  assertFalse(2.0 in 1.0..0.0)

  assertTrue(0.0 in 0.0..<9.0)
  assertFalse(9.0 in 0.0..<9.0)
  assertTrue(5.0 in 0.0..<9.0)
  assertFalse(-1.0 in 0.0..<9.0)
  assertFalse(10.0 in 0.0..<9.0)
  assertFalse(100.0 in 0.0..<9.0)
  assertTrue(-1.0 in -1.0..<1.0)
  assertTrue(0.0 in -1.0..<1.0)
  assertFalse(-2.0 in -1.0..<1.0)
  assertFalse(0.0 in 0.0..<0.0)
  // Ranges with a greater first value are empty.
  assertFalse(0.0 in 1.0..<0.0)
  assertFalse(1.0 in 1.0..<0.0)
  assertFalse(2.0 in 1.0..<0.0)
}

private class ComparableImpl(val first: Int, val second: Int) : Comparable<ComparableImpl> {
  override fun compareTo(other: ComparableImpl): Int =
    if (this.first == other.first) this.second - other.second else this.first - other.first
}

fun testComparableRangeContains() {
  val ranges = ComparableImpl(0, 1)..ComparableImpl(2, 3)
  assertTrue(ComparableImpl(0, 1) in ranges)
  assertTrue(ComparableImpl(1, 10) in ranges)
  assertTrue(ComparableImpl(2, 3) in ranges)
  assertFalse(ComparableImpl(0, 0) in ranges)
  assertFalse(ComparableImpl(2, 4) in ranges)
}

fun testRangeIntrinsics() {
  assertEquals('a'..'z', 'a'..'z')
  assertEquals(1..2, 1..2)
  assertEquals(1..2L, 1..2L)

  assertEquals(('a'..'z').hashCode(), ('a'..'z').hashCode())
  assertEquals((1..2).hashCode(), (1..2).hashCode())
  assertEquals((1..2L).hashCode(), (1..2L).hashCode())

  assertNotEquals('a'..'y', 'a'..'z')
  assertNotEquals(1..3, 1..2)
  assertNotEquals(1..3L, 1..2L)

  // Ranges with a greater first value are empty.
  assertEquals('b'..'a', 'b'..'a')
  assertEquals('b'..'a', 'c'..'a')
  assertEquals(2..1, 2..1)
  assertEquals(2..1, 3..1)
  assertEquals(2L..1L, 2L..1L)
  assertEquals(2L..1L, 3L..1L)

  assertEquals('a'..<'z', 'a'..<'z')
  assertEquals(1..<2, 1..<2)
  assertEquals(1..<2L, 1..<2L)

  assertEquals(('a'..<'z').hashCode(), ('a'..<'z').hashCode())
  assertEquals((1..<2).hashCode(), (1..<2).hashCode())
  assertEquals((1..<2L).hashCode(), (1..<2L).hashCode())

  assertNotEquals('a'..<'y', 'a'..<'z')
  assertNotEquals(1..<3, 1..<2)
  assertNotEquals(1..<3L, 1..<2L)

  // Ranges with a greater first value are empty.
  assertEquals('b'..<'a', 'b'..<'a')
  assertEquals('b'..<'a', 'c'..<'a')
  assertEquals(2..<1, 2..<1)
  assertEquals(2..<1, 3..<1)
  assertEquals(2L..<1L, 2L..<1L)
  assertEquals(2L..<1L, 3L..<1L)
}
