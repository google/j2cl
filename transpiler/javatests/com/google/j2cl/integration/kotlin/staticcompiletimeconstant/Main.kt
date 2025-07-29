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
package staticcompiletimeconstant

import com.google.j2cl.integration.testing.Asserts.assertTrue

val CONST_WITH_ESCAPES = "A\"SD\"F"

// Class with circular static initialization used as a way to test that compile time constants are
// initialized even before $clinit.
object Foo {
  var circular = init()

  const val STRING_COMPILE_TIME_CONSTANT = "qwer"
  const val BYTE_COMPILE_TIME_CONSTANT: Byte = 100
  const val SHORT_COMPILE_TIME_CONSTANT: Short = 100
  const val INT_COMPILE_TIME_CONSTANT = 100
  const val LONG_COMPILE_TIME_CONSTANT = 100L
  const val FLOAT_COMPILE_TIME_CONSTANT = 100f
  const val DOUBLE_COMPILE_TIME_CONSTANT = 100.0
  const val CHAR_COMPILE_TIME_CONSTANT: Char = 100.toChar()
  const val BOOLEAN_COMPILE_TIME_CONSTANT = true

  var initialized: Any? = 1 // Intention boxing to avoid binaryen optimization

  private fun init(): String {
    assertTrue(initialized == null)
    assertTrue(STRING_COMPILE_TIME_CONSTANT === "qwer")
    assertTrue(BYTE_COMPILE_TIME_CONSTANT.toInt() == 100)
    assertTrue(SHORT_COMPILE_TIME_CONSTANT.toInt() == 100)
    assertTrue(INT_COMPILE_TIME_CONSTANT == 100)
    assertTrue(LONG_COMPILE_TIME_CONSTANT == 100L)
    assertTrue(FLOAT_COMPILE_TIME_CONSTANT == 100f)
    assertTrue(DOUBLE_COMPILE_TIME_CONSTANT == 100.0)
    assertTrue(CHAR_COMPILE_TIME_CONSTANT.code == 100)
    assertTrue(BOOLEAN_COMPILE_TIME_CONSTANT == true)

    return Bar.circular
  }
}

// Class with circular static initialization used as a way to test that compile time constants are
// initialized even before $clinit.
object Bar {
  var circular = init()

  const val STRING_COMPILE_TIME_CONSTANT = "qwer"
  const val BYTE_COMPILE_TIME_CONSTANT: Byte = 100
  const val SHORT_COMPILE_TIME_CONSTANT: Short = 100
  const val INT_COMPILE_TIME_CONSTANT = 100
  const val LONG_COMPILE_TIME_CONSTANT: Long = 100
  const val FLOAT_COMPILE_TIME_CONSTANT = 100f
  const val DOUBLE_COMPILE_TIME_CONSTANT = 100.0
  const val CHAR_COMPILE_TIME_CONSTANT: Char = 100.toChar()
  const val BOOLEAN_COMPILE_TIME_CONSTANT = true

  var initialized: Any? = 1 // Intention boxing to avoid binaryen optimization

  private fun init(): String {
    assertTrue(initialized == null)
    assertTrue(STRING_COMPILE_TIME_CONSTANT == "qwer")
    assertTrue(BYTE_COMPILE_TIME_CONSTANT.toInt() == 100)
    assertTrue(SHORT_COMPILE_TIME_CONSTANT.toInt() == 100)
    assertTrue(INT_COMPILE_TIME_CONSTANT == 100)
    assertTrue(LONG_COMPILE_TIME_CONSTANT == 100L)
    assertTrue(FLOAT_COMPILE_TIME_CONSTANT == 100f)
    assertTrue(DOUBLE_COMPILE_TIME_CONSTANT == 100.0)
    assertTrue(CHAR_COMPILE_TIME_CONSTANT.code == 100)
    assertTrue(BOOLEAN_COMPILE_TIME_CONSTANT == true)

    return Foo.circular
  }
}

fun main(vararg unused: String) {
  // Trigger the $clinit;
  val a = Bar.circular

  // Verify that even compile time constants handle string escaping the same as regular strings.
  assertTrue(CONST_WITH_ESCAPES == "A\"SD\"F")
}
