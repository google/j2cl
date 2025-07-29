/*
 * Copyright 2023 Google Inc.
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
package assertsimple

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.fail
import com.google.j2cl.integration.testing.TestUtils.isJvm
import java.lang.AssertionError

/** Test method body, assert statement, and binary expression with number literals work fine. */
// literal boolean expressions
fun main(vararg unused: String) {
  testAssert_succeeds()
  testAssert_fails_differentMessageExpressionTypes()
  // TODO(b/202076599): Remove conditional once bug is fixed.
  if (isJvm()) {
    testAssert_sideEffects()
  }
}

private fun testAssert_succeeds() {
  assert(1 + 2 == 3)
}

private fun testAssert_fails_differentMessageExpressionTypes() {
  try {
    assert(2 == 3)
    fail("Failed to throw assert!")
  } catch (expected: AssertionError) {
    // Success
    assertEquals("Assertion failed", expected.message)
  }

  try {
    assert(2 == 3) { getDescription() }
    fail("Failed to throw assert!")
  } catch (expected: AssertionError) {
    // Success
    assertEquals("custom message", expected.message)
  }

  try {
    assert(2 == 3) { RuntimeException("42") }
    fail("Failed to throw assert!")
  } catch (expected: AssertionError) {
    // Success
    assertEquals("42", expected.cause!!.message)
  }

  try {
    assert(2 == 3) { 42 }
    fail("Failed to throw assert!")
  } catch (expected: AssertionError) {
    // Success
    assertEquals("42", expected.message)
  }

  try {
    assert(2 == 3) { 42L }
    fail("Failed to throw assert!")
  } catch (expected: AssertionError) {
    // Success
    assertEquals("42", expected.message)
  }

  try {
    assert(2 == 3) { true }
    fail("Failed to throw assert!")
  } catch (expected: AssertionError) {
    // Success
    assertEquals("true", expected.message)
  }

  try {
    assert(2 == 3) { 'g' }
    fail("Failed to throw assert!")
  } catch (expected: AssertionError) {
    // Success
    assertEquals("g", expected.message)
  }
}

private fun getDescription() =
  object {
    override fun toString() = "custom message"
  }

private fun testAssert_sideEffects() {
  assertEquals(0, sideEffects)
  assert(1 + 2 == 3) { getMessageWithSideEffects() }
  assertEquals(0, sideEffects)

  try {
    assert(2 == 3) { getMessageWithSideEffects() }
    fail("Failed to throw assert!")
  } catch (expected: AssertionError) {
    // Success
    assertEquals(1, sideEffects)
  }
}

private var sideEffects = 0

private fun getMessageWithSideEffects(): String {
  sideEffects++
  return "message"
}
