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
package privatecrosscall

import com.google.j2cl.integration.testing.Asserts.assertTrue

@SuppressWarnings("ClassShouldBeObject")
class Inner {
  companion object {
    fun caller(): Int {
      return privateMethod()
    }
  }
}

private fun privateMethod(): Int {
  return 3
}

fun main(vararg unused: String) {
  assertTrue(Inner.caller() == 3)
}
