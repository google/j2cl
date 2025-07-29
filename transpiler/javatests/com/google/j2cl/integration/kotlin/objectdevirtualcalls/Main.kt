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
package objectdevirtualcalls

import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue

/**
 * Verifies that Object methods on the Object class execute properly. It is effectively a test that
 * Object method devirtualization is occurring and that the implementation being routed to is
 * proper.
 */
fun main(vararg unused: String) {
  testEquals()
  testHashCode()
  testToString()
  // testGetClass()
}

private val object1 = Any()
private val object2 = Any()

private fun testEquals() {
  assertTrue(object1.equals(object1))
  assertTrue(!object1.equals(object2))
  assertTrue(!object1.equals("some string"))
}

private fun testHashCode() {
  assertTrue(object1.hashCode() != -1)
  assertTrue(object1.hashCode() == object1.hashCode())
  assertTrue(object1.hashCode() != object2.hashCode())
}

private fun testToString() {
  assertTrue(
    object1.toString() == "java.lang.Object@" + java.lang.Integer.toHexString(object1.hashCode())
  )
  assertFalse(object1.toString() == object2.toString())
}

private fun testGetClass() {
  assertTrue(object1.javaClass is Class<*>)
  assertTrue(object1.javaClass == object2.javaClass)
}
