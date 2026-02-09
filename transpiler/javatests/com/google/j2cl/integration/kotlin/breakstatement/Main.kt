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
  testBreakInDoWhileLoop()
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

/**
 * The do-while loops in this test will be rewritten to a while loop with an inner do-while loop. We
 * test that any break statements are still working as expected.
 */
private fun testBreakInDoWhileLoop() {
  // track the expected sequence of loop executions.
  val sequence = mutableListOf<String>()

  fun registerWhileCondition(iteration: Int, loopName: String): Int {
    // Ensure that when break statements are reached, the loop condition is not evaluated.
    sequence.add("while_${loopName}_${iteration}")
    return iteration
  }

  var outer = 0

  outer@ do {
    var x = outer++
    sequence.add("outer_$x")

    // labelled block
    block@ do {
      inner@ do {
        val y = x++
        when (y) {
          0 -> {
            sequence.add("break")
            break
          }
          1 -> {
            sequence.add("break@inner")
            break@inner
          }
          2 -> {
            sequence.add("break@block")

            break@block
          }
          else -> {
            sequence.add("inner_else_$y")
          }
        }
      } while (registerWhileCondition(y, "inner") < 3)
      sequence.add("block")
    } while (false)

    if (x == 4) {
      sequence.add("break_outer")
      break@outer
    }
    sequence.add("end_outer")
  } while (registerWhileCondition(x, "outer") < 100)

  assertEquals(4, outer)

  val expectedSequence =
    listOf(
      "outer_0",
      "break",
      "block",
      "end_outer",
      "while_outer_1",
      "outer_1",
      "break@inner",
      "block",
      "end_outer",
      "while_outer_2",
      "outer_2",
      "break@block",
      "end_outer",
      "while_outer_3",
      "outer_3",
      "inner_else_3",
      "while_inner_3",
      "block",
      "break_outer",
    )
  assertEquals(expectedSequence, sequence)
}
