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
package stringtemplate

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.fail

fun main(vararg unused: String) {
  testStringTemplate()
  testNestedStringTemplate()
  testWithLong()
  testWithNullString()
  testWithValueClass()
  testWithStatementAsExpression()
}

fun foo(a: String, b: Int) = a + b

fun bar(a: String, b: Int) = "$a $b"

fun testStringTemplate() {
  val a = "test"
  assertEquals("test", "$a")
  assertEquals("", "${""}")
  assertEquals("a: test", "a: $a")
  assertEquals("123 456", bar("123", 456))
}

fun testNestedStringTemplate() {
  assertEquals(
    "foo foo0 bar bar 0 ; baz",
    "foo ${foo("foo", 0)} ${"bar ${bar("bar", 0)} ${"; baz"}"}",
  )
}

fun testWithLong() {
  val a = 1
  assertEquals(
    "11111111111111111111111111111111111111111111111111",
    "$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a$a",
  )
}

fun testWithNullString() {
  val a: String? = null
  assertEquals("nullnull", "$a$a")
}

@JvmInline value class SomeValueType(private val i: Int)

fun testWithValueClass() {
  assertEquals("some wrapping SomeValueType(i=1) text", "some wrapping ${SomeValueType(1)} text")
}

fun testWithStatementAsExpression() {
  val i = 0
  val a = "positive i: ${if (i < 0) throw RuntimeException() else i}"
  assertEquals("positive i: 0", a)

  val b = "${return}"
  fail()
}
