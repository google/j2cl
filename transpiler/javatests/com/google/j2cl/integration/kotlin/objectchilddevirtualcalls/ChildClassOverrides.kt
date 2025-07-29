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
package objectchilddevirtualcalls

import com.google.j2cl.integration.testing.Asserts.assertTrue

class ChildClassOverrides {
  override fun equals(o: Any?): Boolean {
    return o is ChildClassOverrides
  }

  override fun hashCode(): Int {
    return 100
  }

  override fun toString(): String {
    return "ChildClassOverrides"
  }

  /** Test object method calls with implicit qualifiers and explicit this. */
  fun test() {
    assertTrue(equals(ChildClassOverrides()))
    assertTrue(!equals(Any()))
    assertTrue(this.equals(ChildClassOverrides()))
    assertTrue(!this.equals(Any()))
    assertTrue(this.hashCode() == 100)
    assertTrue(hashCode() == 100)
    assertTrue(this.toString() == "ChildClassOverrides")
    assertTrue(
      toString() == "ChildClassOverrides",
    )
    assertTrue(this.javaClass is Class<*>)
    assertTrue(javaClass is Class<*>)
  }
}
