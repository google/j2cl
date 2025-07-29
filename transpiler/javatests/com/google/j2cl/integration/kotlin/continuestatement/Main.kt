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
