/*
 * Copyright 2025 Google Inc.
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
package lateinit

import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertNull
import com.google.j2cl.integration.testing.Asserts.assertSame
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.Asserts.fail

fun main(vararg unused: String) {
  testLateinitProperties()
  testLateinitVariable()
}

class LateinitHolder {
  lateinit var f: Any
  private lateinit var privateField: Any

  fun isFInitialized() = this::f.isInitialized

  fun safeGetF() = if (this::f.isInitialized) f else null

  fun isPrivateFieldInitialized() = this::privateField.isInitialized

  fun initializePrivateField(value: Any) {
    privateField = value
  }

  fun safeGetPrivateField() = if (this::privateField.isInitialized) this.privateField else null

  fun unsafeGetPrivateField() = privateField
}

private fun testLateinitProperties() {
  val c = LateinitHolder()

  assertFalse(c.isFInitialized())
  assertFalse(c.isPrivateFieldInitialized())
  assertNull(c.safeGetF())
  assertNull(c.safeGetPrivateField())

  assertThrowsIfNotOptimized {
    val unused = c.f
  }
  assertThrowsIfNotOptimized {
    val unused = c.unsafeGetPrivateField()
  }

  val value = Any()
  c.f = value
  c.initializePrivateField(value)

  assertTrue(c.isFInitialized())
  assertTrue(c.isPrivateFieldInitialized())
  assertSame(value, c.f)
  assertSame(value, c.safeGetPrivateField())
  assertSame(value, c.unsafeGetPrivateField())
}

private fun testLateinitVariable() {
  lateinit var o: Any

  // Note: there's currently no way to check if a lateinit local variable is initialized.
  assertThrowsIfNotOptimized {
    val unused = o
  }

  val value = Any()
  o = value

  assertSame(value, o)
}

private fun assertThrowsIfNotOptimized(action: () -> Unit) {
  // When running compiled with API checks disabled, we will not enforce that the property is
  // initialized.
  if (System.getProperty("COMPILED") == "true") {
    action()
    return
  }

  try {
    action()
    fail()
  } catch (expected: Throwable) {}
}
