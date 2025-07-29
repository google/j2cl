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
package breakstatement

import com.google.j2cl.integration.testing.Asserts.assertEquals

/** Test for for break statement. */
fun main(vararg unused: String) {
  testUnlabeledBreaks()
  testLabeledLoop()
}

private fun testUnlabeledBreaks() {
  var i = 0

  for (j in 0..100) {
    i = j

    if (j == 50) {
      break
    }
  }

  assertEquals(50, i)

  while (true) {
    i++

    if (i == 100) {
      break
    }
  }

  assertEquals(100, i)

  var count = 0

  for (j in 0..100) {
    for (k in 0..10) {
      if (count == 100) {
        break
      }

      count++
    }
  }

  assertEquals(100, count)

  while (true) {
    count++
    break
  }

  assertEquals(101, count)
}

private fun testLabeledLoop() {
  while (true) {
    LABEL@ while (true) {
      break
    }
    break
  }
}
