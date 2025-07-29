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
package nullsafetyoperators

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertNull
import com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.Asserts.fail

fun main(vararg unused: String) {
  testBangBangOperator()
  testSafeCallOperator()
  testElvisOperator()
}

class MyClass(val field: String)

fun testSafeCallOperator() {
  var a: MyClass? = MyClass("?._not_null")
  assertEquals(a?.field, "?._not_null")
  a = null
  assertEquals(a?.field, null)

  val b: MyClass = MyClass("?._not_null2")
  assertEquals(b?.field, "?._not_null2")

  val c: Int = 1
  assertTrue(c?.plus(1) == 2)
}

val TRUE = true

fun testBangBangOperator() {
  var foo: MyClass? = MyClass("!!_not_null")
  assertEquals(foo!!.field, "!!_not_null")

  var bar: MyClass? = null
  var o: Any? = null
  assertThrowsNullPointerException { o = bar!! }
  try {
    o = bar!!.field
    fail("Property access on null should throw")
  } catch (expected: Exception) {}

  // Read o just to make sure the bodies of the try-statements don't get dead code eliminated.
  assertNull(o)

  val b: MyClass = MyClass("!!_not_null3")
  assertEquals(b!!.field, "!!_not_null3")

  val c: Int = 1
  assertEquals(c!!, 1)
}

fun testElvisOperator() {
  var a: String? = null
  assertEquals(elvis(a), "null")

  a = "a"
  assertEquals(elvis(a), "a")

  assertEquals(elvis("c"), "c")

  val c: Int = 1
  assertTrue((c ?: 2) == 1)
}

fun elvis(s: String?): String {
  return s ?: "null"
}

fun testAllInOnce() {
  val nullRef: MyClass? = null
  val nonNullRef: MyClass? = MyClass("all_not_null")

  assertTrue((nullRef?.field ?: nonNullRef!!.field) == "all_not_null")
}
