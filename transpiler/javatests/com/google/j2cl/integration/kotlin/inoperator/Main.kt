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
package inoperator

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testInAsInfixOperatorWithOverload()
  testInWhen()
  testInForLoop()
}

class Holder(val content: Int) {
  operator fun contains(other: Int): Boolean {
    return content == other
  }
}

fun testInAsInfixOperatorWithOverload() {
  assertTrue(1 in Holder(1))
  assertFalse(1 in Holder(2))
  assertTrue(1 !in Holder(2))
  assertFalse(1 !in Holder(1))
}

fun testInWhen() {
  var message =
    when (1) {
      in Holder(1) -> "In Holder"
      else -> "Out of Holder"
    }

  assertEquals("In Holder", message)

  message =
    when (15) {
      !in Holder(1) -> "Out of Holder"
      else -> "In Holder"
    }

  assertEquals("Out of Holder", message)
}

fun testInForLoop() {
  var counter = 1
  for (item in intArrayOf(1, 2, 3)) {
    assertTrue(counter++ == item)
  }
}
