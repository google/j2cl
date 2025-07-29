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
package ifstatement

import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.Asserts.fail

/** Test instanceof class type. */
fun main(vararg unused: String) {
  var count = 0

  if (count == 0) {
    count = 1
  } else {
    fail()
  }

  assertTrue(count == 1)

  if (count == 0) {
    fail()
  } else if (count == 1) {
    count = 2
  } else {
    fail()
  }

  if (count != 2) {
    fail()
  } else if (count == 1) {
    fail()
  } else {
    count = 3
  }

  assertTrue(count == 3)

  // Make sure we add a block for ifs without a block
  if (count == 3) count = 4
  assertTrue(count == 4)

  if (count != 4) fail() else count = 5
  assertTrue(count == 5)

  if (count == 5)  else count = 6
  assertTrue(count == 5)
}
