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
package forstatement

import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Test for for loops. */
fun main(vararg unused: String) {
  var count = 0
  var j = 11
  for (i in 0..9) {
    j++
    count = j + i
  }

  assertTrue(count == 30)

  // Cannot be expressed in a for loop in Kotlin
  // for (count = 0; count == 1; count++) {
  //   count = 100;
  // }
  // assertTrue(count == 0);
  count = 0

  var j2 = 10
  for (i in 0..9) {
    count = j2 - i
    j2--
  }
  assertTrue(count == -8)

  // Cannot be expressed in a for loop in Kotlin
  // for (; count < 10; ) {
  //   count++;
  // }
  // assertTrue(count == 10);
  //
  // for (count = 0; count < 10; count++) ;
  //
  // assertTrue(count == 10);
  count = 10
  OUTER@ for (i in 0..0) {
    while (true) {
      count++
      continue@OUTER
    }
  }
  assertTrue(count == 11)
}
