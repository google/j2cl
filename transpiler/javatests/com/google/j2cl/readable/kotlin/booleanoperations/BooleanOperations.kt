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
package booleanoperations

class BooleanOperations {
  private fun acceptBoolean(b: Boolean) {
    // does nothing
  }

  fun test() {
    var ls = true
    val rs = true
    var r = true
    val t = ! ! !true

    // Assignment.
    r = ls == rs
    r = ls != rs
    r = ls xor rs
    r = ls and rs
    r = ls or rs
    r = ls && rs
    r = ls || rs
    ls = rs
    r = ls

    // boolean compound assignment does not exist in kotlin
    // r ^= rs;
    // r &= rs;
    // r |= rs;

    // Method invocation.
    acceptBoolean(ls == rs)
    acceptBoolean(ls != rs)
    acceptBoolean(ls xor rs)
    acceptBoolean(ls and rs)
    acceptBoolean(ls or rs)
    acceptBoolean(ls && rs)
    acceptBoolean(ls || rs)
    // assignment is not an expression in kotlin
    // acceptBoolean(rs.also { ls = it })

    // No boxed type in kotlin.
    // Boolean br;
    // br = (Boolean) (ls == rs);
    // br = (Boolean) (ls != rs);
    // br = (Boolean) (ls ^ rs);
    // br = (Boolean) (ls & rs);
    // br = (Boolean) (ls | rs);
    // br = (Boolean) (ls && rs);
    // br = (Boolean) (ls || rs);
    // br = (Boolean) (ls = rs);

    // Conditional
    if (ls == rs) {
      r = true
    }
    if (ls != rs) {
      r = true
    }
    if (ls xor rs) {
      r = true
    }
    if (ls and rs) {
      r = true
    }
    if (ls or rs) {
      r = true
    }
    if (ls && rs) {
      r = true
    }
    if (ls || rs) {
      r = true
    }

    // Compound assignment with enclosing instance.
    class Outer {
      var b = 0

      inner class Inner {
        init {
          b += 1
        }
      }
    }

    val finalOuter = Outer()
    finalOuter.b += 1

    var outer = Outer()
    // assignment is not an expression in kotlin
    // outer.b = outer.b or (outer = null) == null
  }
}
