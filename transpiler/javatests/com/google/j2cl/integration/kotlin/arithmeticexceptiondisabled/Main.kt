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
package arithmeticexceptiondisabled

import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  val a = 10
  val b = 0
  // This should fail! But we've turned off arithmetic exception in the BUILD file.
  val c = a / b

  // Use the resulting value, to prove that size reductions are not accidentally because
  // the object was unused and JSCompiler decided to delete the cast on an unused thing.
  assertTrue(c < 100)
  assertTrue(c > -100)
}
