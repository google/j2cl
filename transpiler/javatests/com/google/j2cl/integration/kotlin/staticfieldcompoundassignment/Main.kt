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
package staticfieldcompoundassignment

import com.google.j2cl.integration.testing.Asserts.assertTrue

var bar: Long = 0

fun main(vararg unused: String) {
  // If there were $q qualified static field references in the desugaring of this compound
  // assignment, then the CollapseProperties optimization pass would break them and the following
  // assertion would fail.
  bar++

  assertTrue(bar == 1L)
}
