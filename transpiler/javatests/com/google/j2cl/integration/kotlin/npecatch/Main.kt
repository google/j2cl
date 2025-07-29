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
package npecatch

import com.google.j2cl.integration.testing.Asserts.fail

/** Test NPE catch. */
fun main(vararg unused: String) {
  try {
    throwNpe()
    fail()
  } catch (expected: NullPointerException) {
    // expected
  }
}

internal class SomeObject {
  fun doX(): String {
    return "$this x"
  }
}

fun throwNpe() {
  val a: SomeObject? = null
  a!!.doX()
}
