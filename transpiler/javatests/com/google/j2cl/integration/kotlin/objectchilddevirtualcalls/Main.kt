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
package objectchilddevirtualcalls

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue

/**
 * Verifies that Object methods on the ChildClass class execute properly. It is effectively a test
 * that Object method devirtualization is occurring even for children of Object and that the
 * implementation being routed to is proper.
 */
fun main(vararg unused: String) {
  testEquals()
  testHashCode()
  testToString()
  testGetClass()
  testChildClassOverrides()
}

private val childClass1: ChildClass = ChildClass()
private val childClass2: ChildClass = ChildClass()

private fun testEquals() {
  assertTrue(childClass1.equals(childClass1))
  assertFalse(childClass1.equals(childClass2))
  assertFalse(childClass1.equals("some string"))
}

private fun testHashCode() {
  assertTrue(childClass1.hashCode() != -1)
  assertTrue(childClass1.hashCode() == childClass1.hashCode())
  assertTrue(childClass1.hashCode() != childClass2.hashCode())
}

private fun testToString() {
  assertEquals(
    "objectchilddevirtualcalls.ChildClass@" + java.lang.Integer.toHexString(childClass1.hashCode()),
    childClass1.toString(),
  )
  assertFalse(childClass1.toString().equals(childClass2.toString()))
}

private fun testGetClass() {
  assertTrue(childClass1.javaClass is Class<*>)
  assertTrue(childClass1.javaClass === childClass2.javaClass)
}

private fun testChildClassOverrides() {
  ChildClassOverrides().test()
}
