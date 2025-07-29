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
package compiletimeconstant

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertNull
import com.google.j2cl.integration.testing.Asserts.assertTrue

/**
 * This test verifies that compile time constants can be accessed and that they don't call the
 * clinit.
 */
fun main(vararg unused: String) {
  // TODO(b/232172836): Add tests with constants in companion object.
  testClinitOrder()
}

var ranClinit = false

private fun testClinitOrder() {
  var total = 0
  total += CONSTANT

  assertFalse(ranClinit)
  assertEquals(10, total)
  total += CONSTANT_PLUS_ONE // 11 + 10

  assertFalse(ranClinit)
  assertEquals(21, total)
  total += CONSTANT_COMPOSED // 21 + 11 + 10

  assertFalse(ranClinit)
  assertEquals(42, total)
  if (CONSTANT_COMPOSED_STRING != "") {
    total += CONSTANT_COMPOSED_STRING.length // 42 + 16
  }

  assertFalse(ranClinit)
  assertEquals(58, total)
  total += nonConstant

  assertTrue(Char.MAX_VALUE.code == 65535)
  assertTrue(Char.MIN_VALUE.code.toByte() > Char.MAX_VALUE.code.toByte())
  assertTrue(Char.MIN_VALUE.code.toShort() > Char.MAX_VALUE.code.toShort())
  assertTrue(Char.MIN_VALUE.code.toLong() == 0L)
  assertTrue(Char.MIN_VALUE.code.toFloat() == 0f)
  assertTrue(Char.MIN_VALUE.code.toDouble() == 0.0)

  val maxUByteLimit: UByte = UByte.MAX_VALUE
  assertTrue(maxUByteLimit == UByte.MAX_VALUE)
  val maxUShortLimit: UShort = UShort.MAX_VALUE
  assertTrue(maxUShortLimit == UShort.MAX_VALUE)
  val maxUIntLimit: UInt = UInt.MAX_VALUE
  assertTrue(maxUIntLimit == UInt.MAX_VALUE)
  val maxULongLimit: ULong = ULong.MAX_VALUE
  assertTrue(maxULongLimit == ULong.MAX_VALUE)

  // TODO(b/378892483): Re-enable this test when the IR deserializer is fixed.
  // assertTrue(UByte.MIN_VALUE < UByte.MAX_VALUE)
  // assertTrue(UShort.MIN_VALUE < UShort.MAX_VALUE)
  // assertTrue(UInt.MIN_VALUE < UInt.MAX_VALUE)
  // assertTrue(ULong.MIN_VALUE < UInt.MAX_VALUE)

  assertTrue(ranClinit)
  assertEquals(78, total)
  assertNull(OBJ)

  ranClinit = false
  total += CompileTimeConstantInterface.CONSTANT
  assertFalse(ranClinit)
  assertTrue(total == 79)
  assertTrue(CompileTimeConstantInterface.OBJ == null)
  assertTrue(ranClinit)
}
