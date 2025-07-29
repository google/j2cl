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
package assertsimple

class AssertSimple {
  fun main(args: Array<String>) {
    assert(1 + 2 == 3)
    assert(1 + 2 == 3) { "Error!" }
    assert(1 + 2 == 3) { args }
    assert(1 + 2 == 3) { args[0] }
    assert(1 + 2 == 3) { Any() }
  }
}
