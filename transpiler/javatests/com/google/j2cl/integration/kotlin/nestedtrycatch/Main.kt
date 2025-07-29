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
package nestedtrycatch

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.fail

var count = 1

/** Test nested try catch. */
fun main(vararg unused: String) {
  try {
    throwRuntimeException()
  } catch (ae: RuntimeException) {
    count = 2
    try {
      notThrowException()
    } catch (ie: ClassCastException) {
      fail() // No exception was thrown so shouldn't reach catch block.
    }
  } finally {
    assertEquals(2, count) // first catch block is executed.
  }
}

@Throws(RuntimeException::class)
fun throwRuntimeException() {
  throw RuntimeException()
}

@Throws(ClassCastException::class) fun notThrowException() {}
