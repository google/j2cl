/*
 * Copyright 2025 Google Inc.
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
package tailrec

import com.google.j2cl.integration.testing.Asserts.assertEquals

fun main(vararg unused: String) {
  testFactorial()
}

fun testFactorial() {
  assertEquals(120, factorialTC(5))
  assertEquals(120, factorialInvalidTC(5))
}

tailrec fun factorialInvalidTC(i: Int): Int {
  if (i == 0) return 1
  // This is not a valid Tail Recursion call. Kotlinc will generate a warning, but it will still
  // compile.
  return i * factorialInvalidTC(i - 1)
}

tailrec fun factorialTC(i: Int, result: Int = 1): Int {
  if (i == 0) return result
  return factorialTC(i - 1, i * result)
}
