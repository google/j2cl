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
package stringdevirtualcalls

import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue

/**
 * Verifies that String methods on the String class execute properly. It is effectively a test that
 * String method devirtualization is occurring and that the implementation being routed to is
 * proper.
 */
fun main(vararg unused: String) {
  testEquals()
  testHashCode()
  testToString()
  testGetClass()
  testComparableMethods()
  testCharSequenceMethods()
  testStringMethods()
}

private fun testEquals() {
  val string1 = "string1"
  val string2 = "string2"

  assertTrue(string1.equals(string1))
  assertFalse(string1.equals(string2))
  assertFalse(string1.equals(Any()))
}

private fun testHashCode() {
  val string1 = "string1"
  val string2 = "string2"
  assertTrue(string1.hashCode() != -1)
  assertTrue(string1.hashCode() == string1.hashCode())
  assertTrue(string1.hashCode() != string2.hashCode())
}

private fun testToString() {
  val string1 = "string1"
  val string2 = "string2"
  assertTrue(string1.toString() !== null)
  assertTrue(string1.toString() === string1)
  assertTrue(string1.toString() !== string2)
}

private fun testGetClass() {
  val string1 = "string1"
  val string2 = "string2"
  assertTrue(string1.javaClass is Class<*>)
  assertTrue(string1.javaClass == string2.javaClass)
}

private fun testComparableMethods() {
  val string1 = "string1"
  val string2 = "string2"
  assertTrue(string1.compareTo(string2) == -1)
  assertTrue(string1.compareTo(string1) == 0)
  assertTrue(string2.compareTo(string1) == 1)
  assertTrue(string2.compareTo(string2) == 0)
}

private fun testCharSequenceMethods() {
  val string1 = "string1"
  val string2 = ""
  assertTrue(string1.length == 7)
  assertTrue(string2.length == 0)
  assertTrue(string1[0] == 's')
  assertTrue(string1[1] == 't')
  assertTrue(string1[2] == 'r')
  assertTrue(string1[3] == 'i')
  assertTrue(string1[4] == 'n')
  assertTrue(string1[5] == 'g')
  assertTrue(string1[6] == '1')
  assertTrue(string1.get(6) == '1')
  assertTrue(string1.subSequence(0, 2).equals("st"))
}

private fun testStringMethods() {
  val string1 = "string1"
  var whitespaceString = string1 + " "

  assertTrue(whitespaceString.trim() == string1)

  whitespaceString = "  " + string1 + " "

  assertTrue(whitespaceString.trim() == string1)

  assertTrue(string1.substring(0) == "string1")
  assertTrue(string1.substring(1) == "tring1")
  assertTrue(string1.substring(2) == "ring1")
  assertTrue(string1.substring(3) == "ing1")
  assertTrue(string1.substring(4) == "ng1")
  assertTrue(string1.substring(5) == "g1")
  assertTrue(string1.substring(6) == "1")
  assertTrue(string1.substring(7) == "")

  assertTrue(string1.substring(0, 2) == "st")
  assertTrue(string1.substring(1, 5) == "trin")
}
