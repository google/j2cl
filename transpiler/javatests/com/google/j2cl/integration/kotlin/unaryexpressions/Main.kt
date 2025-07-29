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
package unaryexpressions

import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Test unary operations. */
fun main(vararg unused: String) {
  testBasicUnaryExpressions()
  testMinusZero()
}

private fun testBasicUnaryExpressions() {
  var a = 10
  assertTrue(a == 10)

  val b = a++
  assertTrue(a == 11)
  assertTrue(b == 10)

  val c = a--
  assertTrue(a == 10)
  assertTrue(c == 11)

  val d = ++a
  assertTrue(a == 11)
  assertTrue(d == 11)

  val e = --a
  assertTrue(a == 10)
  assertTrue(e == 10)

  val f = -a
  assertTrue(f == -10)

  val g = +a
  assertTrue(g == 10)

  // Kotlin has no operator syntax for complement (~a in Java).
  val h = a.inv()
  assertTrue(h == -11)

  val i = (a == 100)
  assertTrue(!i)

  var s: Short = 10
  // Test that the result of evaluating "++s" is correct.
  // DO NOT CHANGE to "assertEquals(++s, 11)" since the tests also make sure that the expressions
  // are transformed correctly w.r.t. operator precedence.
  assertTrue(++s == 11.toShort())
  // Test that ++s correctly mutates "s". Also indirectly tests that "++s == 11" is not
  // incorrectly expanded to "s = s + 1 == 11", which would have resulted in "s" having "true" as
  // its value.
  assertTrue(s == 11.toShort())

  // Test that the result of evaluating "s++" is correct.
  assertTrue(s++ == 11.toShort())
  // Test that ++s correctly mutates "s".
  assertTrue(s == 12.toShort())

  var ints = intArrayOf(1, 2, 3)
  // Test that the result of evaluating "++ints[]" is correct.
  // DO NOT CHANGE to "assertEquals(++ints[1], 3)" since the tests also make sure that the
  // expressions are transformed correctly w.r.t. operator precedence.
  assertTrue(++ints[1] == 3)
  // Test that ++ints[1] correctly mutates "ints[1]". Also indirectly tests that "++ints[1] == 11"
  // is not incorrectly expanded to "ints[1] = ints[1] + 1 == 11", which would have resulted in
  // "ints[1]" having "true" as its value.
  assertTrue(ints[1] == 3)

  // TODO(b/446802760): Uncomment when companion objects are supported.
  // val l: Double = -0x80000000.toDouble() // -Integer.MIN_VALUE
  // assertTrue(l == Integer.MIN_VALUE.toDouble())

  // Test space after - and + unary operators, so they don't behave like -- and ++.
  assertTrue(-(-1) == 1)
  assertTrue(-(-(1)) == 1)
  assertTrue(+(+1) == 1)
  assertTrue(+(+(1)) == 1)
  assertTrue(-(-1.0) == 1.0)
  assertTrue(-(-(1.0)) == 1.0)
  assertTrue(+(+1.0) == 1.0)
  assertTrue(+(+(1.0)) == 1.0)
}

fun testMinusZero() {
  assertFalse(1 / -0.0 == 1 / 0.0)
}
