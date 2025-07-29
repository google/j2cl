/*
 * Copyright 2023 Google Inc.
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
package blockscoping

import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  // If 'i' is not block scoped then Closure compile will fail and if the 'i' created in the
  // 'for' header is not available in the 'for' body then runtime will fail.
  for (i in 11..11) {
    assertTrue(i == 11)
  }

  // If 'i' is not block scoped then Closure compile will fail.
  var firstTime = true
  while (firstTime) {
    val i = 22
    assertTrue(i == 22)
    firstTime = false
  }

  // If 'i' is not block scoped then Closure compile will fail.
  if (!firstTime) {
    val i = 33
    assertTrue(i == 33)
  }

  // If 'i' is not block scoped then Closure compile will fail.
  run {
    val i = 44
    assertTrue(i == 44)
  }

  var x: Int
  run { x = 1 }
  x = x + 1
  assertTrue(x == 2)
}
