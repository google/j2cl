/*
 * Copyright 2015 Google Inc.
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
package labeledstatement

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Test for labeled statements. */
fun main(vararg unused: String) {
  testLabeledLoop()
  testNestedLabeledLoop()
}

private fun testLabeledLoop() {
  var i = 0
  var j = 0

  label@ while (++i < 100) {
    j = 0
    while (++j < 10) {
      if (i == 10 && j == 5) {
        break@label
      }
    }
  }

  assertEquals(10, i)
  assertEquals(5, j)

  i = 0
  j = 0
  var count = 0

  // Use the same label as before. Labels need only to be unique from the nesting perspective.
  label@ while (++i < 100) {
    j = 0
    while (++j < 10) {
      if (j == 5) {
        continue@label
      }
      count++
    }
  }

  assertEquals(100, i)
  assertEquals(5, j)
  assertEquals(99 * 4, count) // 99 (outer loop) * 4 (inner loop)

  count = 0
  var unreachable = true
  skip@ do {
    if (count == 0) {
      break@skip // jumps to the end of the labeled block.
    }
    unreachable = false
  } while (false)
  assertTrue(unreachable)

  count = 0
  breakcontinue@ while (true) {
    if (++count > 1) {
      break@breakcontinue
    }

    continue@breakcontinue
  }
  assertEquals(2, count)
}

private fun testNestedLabeledLoop() {
  // Breaking outer loop
  var count = 0
  outer@ while (true) {
    while (true) {
      if (count == 5) break@outer
      count++
    }
    count++
  }

  assertEquals(5, count)

  // Breaking inner loop
  var count2 = 0
  outer@ while (true) {
    inner@ while (true) {
      if (count2 == 5) break@inner
      count2++
    }
    count2++
    break
  }

  assertEquals(6, count2)

  // Do-while
  var count3 = 0
  outer@ do {
    do {
      if (count3 == 5) break@outer
      count3++
    } while (true)
    count3++
  } while (true)

  assertEquals(5, count3)
}
