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
package arithmeticexception

import com.google.j2cl.integration.testing.Asserts.assertThrowsArithmeticException

fun main(vararg unused: String) {
  testDivideByZero()
  testModByZero()
}

private fun testDivideByZero() {
  assertThrowsArithmeticException {
    val a = 10
    val b = 0
    val unused = a / b
  }

  assertThrowsArithmeticException {
    var unused = 10
    val b = 0
    unused /= b
  }

  assertThrowsArithmeticException {
    val a = 10
    val b = 0
    val unused = 1 + a / b
  }

  assertThrowsArithmeticException {
    val a: Short = 10
    val b: Short = 0
    val unused = (a / b).toShort()
  }

  // No division operation available on Char
  // assertThrowsArithmeticException {
  //   val a = 10.toChar()
  //   val b = 0.toChar()
  //   val unused: Char = (a / b).toChar()
  // }
  assertThrowsArithmeticException {
    val a: Byte = 10
    val b: Byte = 0
    val unused = (a / b).toByte()
  }

  assertThrowsArithmeticException {
    val a: Long = 10
    val b: Long = 0
    val unused = (a / b)
  }

  assertThrowsArithmeticException {
    val a = 10
    val b = 0
    val unused = 10L + (a / b)
  }

  assertThrowsArithmeticException {
    val a: Short = 10
    val b: Short = 0
    val unused = 10L + (a / b)
  }
}

private fun testModByZero() {
  assertThrowsArithmeticException {
    val a = 10
    val b = 0
    val unused = a % b
  }

  assertThrowsArithmeticException {
    var unused = 10
    val b = 0
    unused %= b
  }

  assertThrowsArithmeticException {
    val a: Short = 10
    val b: Short = 0
    val unused = (a % b).toShort()
  }

  // No modulo operation available on Char
  // assertThrowsArithmeticException {
  //   val a = 10.toChar()
  //   val b = 0.toChar()
  //   val unused: Char = (a % b).toChar()
  // }

  assertThrowsArithmeticException {
    val a: Byte = 10
    val b: Byte = 0
    val unused = (a % b).toByte()
  }

  assertThrowsArithmeticException {
    val a: Long = 10
    val b: Long = 0
    val unused = (a % b)
  }

  assertThrowsArithmeticException {
    val a = 10
    val b = 0
    val unused = 10L + (a % b)
  }

  assertThrowsArithmeticException {
    val a: Short = 10
    val b: Short = 0
    val unused = 10.0 + (a % b)
  }
}
