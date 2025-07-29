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

val b: Int =
  if (true) {
    // Observe `A.kt` static state which has not been initialized yet.
    assertTrue(a == 0)
    assertTrue(initializedToDefault == 0)
    assertTrue(initializedToTen == 0)

    initializedToDefault = 5
    initializedToTen = 5
    5
  } else throw AssertionError()
