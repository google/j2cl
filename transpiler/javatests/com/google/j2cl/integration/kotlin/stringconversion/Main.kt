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
package stringconversion

import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.TestUtils.getUndefined

private class Person(private val firstName: String, private val lastName: String) {
  override fun toString(): String {
    return "$firstName $lastName"
  }
}

fun main(vararg unused: String) {
  val locationString = "California, USA"
  val samPerson = Person("Sam", "Smith")
  val nullPerson: Person? = null

  // Object with toString() override.
  var result = samPerson.toString() + " is located in " + locationString
  assertTrue(result == "Sam Smith is located in California, USA")
  result = "$samPerson is located in $locationString"
  assertTrue(result == "Sam Smith is located in California, USA")

  // Null Object instance.
  result = nullPerson?.toString() + " is located in nowhere"
  assertTrue(result == "null is located in nowhere")
  result = "$nullPerson is located in nowhere"
  assertTrue(result == "null is located in nowhere")

  // Two Null String instances.
  var s1: String? = null
  var s2: String? = null
  var s3 = s1 + s2 // two nullable string instances
  assertTrue(s3 == "nullnull")
  s2 += s2 // nullable string compound assignment, plus a nullable string.
  assertTrue(s2 == "nullnull")
  s1 += "a" // nullable string compound assignment, plus a string literal.
  assertTrue(s1 == "nulla")

  s1 = null
  s3 = s1 + s1 + s1 + null + "a"
  assertTrue(s3 == "nullnullnullnulla")
  s3 = "a" + s1 + s1 + s1 + null
  assertTrue(s3 == "anullnullnullnull")

  // Char + String
  val c1 = 'F'
  val c2 = 'o'
  val i = (c1.code + c2.code).toString()
  assertTrue((c1.code + c2.code).toString() + "o" == i + "o")

  // Boolean boxed type as JS primitive.
  result = true.toString() + " is not " + false
  assertTrue(result == "true is not false")

  // with integer binary operations
  result = (1 + 2).toString() + "Foo" + 3 + 2
  assertTrue(result == "3Foo32")

  val charArray = charArrayOf('f', 'o', 'o')
  assertFalse("barfoo" == "bar" + charArray)

  testCharSequenceConcatenation()
  testPrimitiveConcatenation()
  testConcatenationWithUndefined()
}

private class SimpleCharSequence : CharSequence {
  override fun toString(): String {
    return "some string"
  }

  override val length: Int
    get() = throw UnsupportedOperationException()

  override fun get(index: Int): Char {
    throw UnsupportedOperationException()
  }

  override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
    throw UnsupportedOperationException()
  }
}

private fun testCharSequenceConcatenation() {
  val stringBuilder = StringBuilder("foo").append("bar")
  assertTrue(stringBuilder.toString() + "baz" == "foobarbaz")

  val stringBuffer = StringBuffer("foo").append("bar")
  assertTrue(stringBuffer.toString() + "baz" == "foobarbaz")

  assertTrue(
    SimpleCharSequence().toString() + " from SimpleCharSequence" ==
      "some string from SimpleCharSequence"
  )
}

private fun testPrimitiveConcatenation() {
  // Test both the statically evaluated string literals and the regular concat on different types.
  val bool = true
  assertTrue("$bool is true" == "true is true")
  assertTrue("true is " + true == "true is true")
  val s: Short = 1
  assertTrue("$s is 1" == "1 is 1")
  assertTrue("1 is " + 1.toShort() == "1 is 1")
  val b: Byte = 1
  assertTrue("$b is 1" == "1 is 1")
  assertTrue("1 is " + 1.toByte() == "1 is 1")
  val c = 'F'
  assertTrue("$c is F" == "F is F")
  assertTrue("F is " + 'F' == "F is F")
  val i = 1
  assertTrue("$i is 1" == "1 is 1")
  assertTrue("1 is " + 1 == "1 is 1")
  val l = 1L
  assertTrue("$l is 1" == "1 is 1")
  assertTrue("1 is " + 1L == "1 is 1")
  val d = 1.1
  assertTrue("$d is 1.1" == "1.1 is 1.1")
  assertTrue("1.1 is " + 1.1 == "1.1 is 1.1")
  val f = 1.5f
  assertTrue("$f is 1.5" == "1.5 is 1.5")
  assertTrue("1.5 is " + 1.5f == "1.5 is 1.5")
}

private fun testConcatenationWithUndefined() {
  val s1: String? = null
  val s2 = getUndefined<String>() // undefined in JavaScript
  val s3 = s1 + s2 // two nullable string instances
  assertTrue(s3 == "nullnull")
}
