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
package cyclicclinits

import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Test cyclic $clinits. */
fun main(vararg unused: String) {
  // call on companion object triggers Child.$clinit(), which then triggers Parent.$clinit().
  // In Parent.$clinit(), it refers to Child companion field, while this does not trigger
  // Child.$clinit().
  Child.test()

  // Will trigger the A.kt clinit which reference B.b and so triggers B.kt clinit. B.kt clinit
  // refers to A.a while this does not trigger A.kt clinit.
  assertTrue(a == 50)
  // The values in these fields are written before the clinit is executed.
  // Field initialized to its default value during declaration will see the values written before
  // the clinit.
  assertTrue(initializedToDefault == 5)
  // The value is overwritten by the clinit due to having an initializer.
  assertTrue(initializedToTen == 10)
}
