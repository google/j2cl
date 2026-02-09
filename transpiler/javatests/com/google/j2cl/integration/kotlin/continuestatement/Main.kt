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
package continuestatement

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.Asserts.fail

/** Test for continue statement. */
fun main(vararg unused: String) {
  testForLoop()
  testWhileLoop()
  testNestedForLoop()
  testNestedSwitch()
  testContinueInDoWhileLoop()
}

private fun testForLoop() {
  var k = 0

  for (i in 0..100) {
    k = i
    if (i < 50) {
      continue
    }
    break
  }
  assertEquals(50, k)
}

private fun testWhileLoop() {
  var i = 0

  while (true) {
    i++

    if (i < 100) {
      continue
    }
    break
  }

  assertEquals(100, i)
}

private fun testNestedForLoop() {
  var count = 0

  for (j in 0..100) {
    for (k in 0..10) {
      count++
      if (count < 100) {
        continue
      }
      break
    }
    if (count < 100) {
      continue
    }
    break
  }

  assertEquals(100, count)

  // Infinite for loops not supported in Kotlin.
  // for (; ; ) {
  //   count++
  //   if (count < 101) {
  //     continue
  //   }
  //   break
  // }
  //
  // assertEquals(101, count)
}

private fun testNestedSwitch() {
  var loop = true

  while (loop) {
    when (1) {
      1 -> {
        when (2) {
          2 -> {
            assertTrue(loop)
            loop = false
            continue
          }
        }
      }
    }
    fail()
  }
}

/**
 * The do-while loops in this test will be rewritten to a while loop with an inner do-while loop. We
 * test that any continue statements are still working as expected.
 */
private fun testContinueInDoWhileLoop() {
  // track the expected sequence of loop executions.
  val sequence = mutableListOf<String>()

  fun registerWhileCondition(iteration: Int, loopName: String): Int {
    // Ensure that when continue statements are reached, the loop condition is still evaluated.
    sequence.add("while_${loopName}_${iteration}")
    return iteration
  }

  var outer = 0

  outer@ do {
    var x = outer++
    sequence.add("outer_$x")

    inner@ do {
      val y = x++ // 2 3
      when (y) {
        0 -> {
          sequence.add("continue")
          continue
        }
        1 -> {
          sequence.add("continue@inner")
          continue@inner
        }
        else -> {
          sequence.add("inner_else_$y")
        }
      }
    } while (registerWhileCondition(y, "inner") < 2)

    if (x < 4) {
      sequence.add("continue_outer")
      continue@outer
    }
    sequence.add("Should not be called")
  } while (registerWhileCondition(x, "outer") < 3)

  assertEquals(1, outer)

  val expectedSequence =
    listOf(
      "outer_0",
      "continue",
      "while_inner_0",
      "continue@inner",
      "while_inner_1",
      "inner_else_2",
      "while_inner_2",
      "continue_outer",
      "while_outer_3",
    )
  assertEquals(expectedSequence, sequence)
}
