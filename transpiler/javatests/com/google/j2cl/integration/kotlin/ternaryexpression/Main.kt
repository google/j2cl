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
package ternaryexpression

import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Test ternary expression. */
fun main(vararg unused: String) {
  var count = if (true) 1 else 2
  assertTrue(count == 1)

  count = if (count == 2) 2 else 3
  assertTrue(count == 3)

  var foo = if (2 * count / 2 == 3) (4 + 4) / 2 else 5 + 5
  assertTrue(foo == 4)

  foo = if (foo < 5 && foo > 3) if (foo == 4) 5 else 6 else 0
  assertTrue(foo == 5)

  foo = if (foo == 5) 15 else 30
  assertTrue(foo == 15)
}
