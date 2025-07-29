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
package interfacedevirtualize

import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Test Comparable Interface on all devirtualized classes that implement it. */
fun testComparable() {

  testDouble()
  testBoolean()
  testInteger()
  testLong()
  testString()
  testComparableImpl()
}

private fun <T> compare0(c: Comparable<T>?, t: T?): Int {
  return c!!.compareTo(t!!)
}

@SuppressWarnings("unchecked")
private fun compare1(c: Comparable<*>?, o: Any?): Int {
  return (c as Comparable<Any?>).compareTo(o)
}

private fun compare2(c: Comparable<Double>?, d: Double?): Int {
  return c!!.compareTo(d!!)
}

private fun compare2(c: Comparable<Boolean>?, b: Boolean?): Int {
  return c!!.compareTo(b!!)
}

private fun compare2(c: Comparable<Int>?, i: Int?): Int {
  return c!!.compareTo(i!!)
}

private fun compare2(c: Comparable<Long>, l: Long?): Int {
  return c.compareTo(l!!)
}

private fun compare2(s: Comparable<String>, si: String): Int {
  return s.compareTo(si)
}

private fun compare2(c: Comparable<ComparableImpl>, ci: ComparableImpl): Int {
  return c.compareTo(ci)
}

private fun compare3(d1: Double?, d2: Double?): Int {
  return d1!!.compareTo(d2!!)
}

private fun compare3(b1: Boolean?, b2: Boolean?): Int {
  return b1!!.compareTo(b2!!)
}

private fun compare3(i1: Int?, i2: Int?): Int {
  return i1!!.compareTo(i2!!)
}

private fun compare3(l1: Long?, l2: Long?): Int {
  return l1!!.compareTo(l2!!)
}

private fun compare3(s1: String, s2: String): Int {
  return s1.compareTo(s2)
}

private fun compare3(c1: ComparableImpl, c2: ComparableImpl): Int {
  return c1.compareTo(c2)
}

private fun testDouble() {
  val d1: Double? = 1.1
  val d2: Double? = 1.1
  val d3: Double? = 2.1
  assertTrue(compare0(d1, d2) == 0)
  assertTrue(compare0(d1, d3) < 0)
  assertTrue(compare0(d3, d2) > 0)

  assertTrue(compare1(d1, d2) == 0)
  assertTrue(compare1(d1, d3) < 0)
  assertTrue(compare1(d3, d2) > 0)

  assertTrue(compare2(d1, d2) == 0)
  assertTrue(compare2(d1, d3) < 0)
  assertTrue(compare2(d3, d2) > 0)

  assertTrue(compare3(d1, d2) == 0)
  assertTrue(compare3(d1, d3) < 0)
  assertTrue(compare3(d3, d2) > 0)

  assertThrowsClassCastException { compare1(d1, Any()) }
}

private fun testBoolean() {
  val b1 = false
  val b2 = false
  val b3 = true
  assertTrue(compare0(b1, b2) == 0)
  assertTrue(compare0(b1, b3) < 0)
  assertTrue(compare0(b3, b2) > 0)

  assertTrue(compare1(b1, b2) == 0)
  assertTrue(compare1(b1, b3) < 0)
  assertTrue(compare1(b3, b2) > 0)

  assertTrue(compare2(b1, b2) == 0)
  assertTrue(compare2(b1, b3) < 0)
  assertTrue(compare2(b3, b2) > 0)

  assertTrue(compare3(b1, b2) == 0)
  assertTrue(compare3(b1, b3) < 0)
  assertTrue(compare3(b3, b2) > 0)

  assertThrowsClassCastException { compare1(b1, Any()) }
}

private fun testInteger() {
  val i1: Int? = 1000
  val i2: Int? = 1000
  val i3: Int? = 2000
  assertTrue(compare0(i1, i2) == 0)
  assertTrue(compare0(i1, i3) < 0)
  assertTrue(compare0(i3, i2) > 0)

  assertTrue(compare1(i1, i2) == 0)
  assertTrue(compare1(i1, i3) < 0)
  assertTrue(compare1(i3, i2) > 0)

  assertTrue(compare2(i1, i2) == 0)
  assertTrue(compare2(i1, i3) < 0)
  assertTrue(compare2(i3, i2) > 0)

  assertTrue(compare3(i1, i2) == 0)
  assertTrue(compare3(i1, i3) < 0)
  assertTrue(compare3(i3, i2) > 0)

  assertThrowsClassCastException { compare1(i1, Any()) }
}

private fun testLong() {
  val l1 = 1000L
  val l2 = 1000L
  val l3 = 2000L
  assertTrue(compare0(l1, l2) == 0)
  assertTrue(compare0(l1, l3) < 0)
  assertTrue(compare0(l3, l2) > 0)

  assertTrue(compare1(l1, l2) == 0)
  assertTrue(compare1(l1, l3) < 0)
  assertTrue(compare1(l3, l2) > 0)

  assertTrue(compare2(l1, l2) == 0)
  assertTrue(compare2(l1, l3) < 0)
  assertTrue(compare2(l3, l2) > 0)

  assertTrue(compare3(l1, l2) == 0)
  assertTrue(compare3(l1, l3) < 0)
  assertTrue(compare3(l3, l2) > 0)

  assertThrowsClassCastException { compare1(l1, Any()) }
}

private fun testString() {
  val s1 = "abc"
  val s2 = "abc"
  val s3 = "def"
  assertTrue(compare0(s1, s2) == 0)
  assertTrue(compare0(s1, s3) < 0)
  assertTrue(compare0(s3, s2) > 0)

  assertTrue(compare1(s1, s2) == 0)
  assertTrue(compare1(s1, s3) < 0)
  assertTrue(compare1(s3, s2) > 0)

  assertTrue(compare2(s1, s2) == 0)
  assertTrue(compare2(s1, s3) < 0)
  assertTrue(compare2(s3, s2) > 0)

  assertTrue(compare3(s1, s2) == 0)
  assertTrue(compare3(s1, s3) < 0)
  assertTrue(compare3(s3, s2) > 0)

  assertThrowsClassCastException { compare1(s1, Any()) }
}

private fun testComparableImpl() {
  val c1 = ComparableImpl(1000)
  val c2 = ComparableImpl(1000)
  val c3 = ComparableImpl(2000)
  assertTrue(compare0<ComparableImpl>(c1, c2) == 0)
  assertTrue(compare0<ComparableImpl>(c1, c3) < 0)
  assertTrue(compare0<ComparableImpl>(c3, c2) > 0)

  assertTrue(compare1(c1, c2) == 0)
  assertTrue(compare1(c1, c3) < 0)
  assertTrue(compare1(c3, c2) > 0)

  assertTrue(compare2(c1, c2) == 0)
  assertTrue(compare2(c1, c3) < 0)
  assertTrue(compare2(c3, c2) > 0)

  assertTrue(compare3(c1, c2) == 0)
  assertTrue(compare3(c1, c3) < 0)
  assertTrue(compare3(c3, c2) > 0)

  assertThrowsClassCastException { compare1(c1, Any()) }
}
