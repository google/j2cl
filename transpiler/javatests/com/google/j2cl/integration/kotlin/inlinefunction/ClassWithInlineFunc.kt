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
package inlinefunction

import com.google.j2cl.integration.testing.Asserts.assertTrue

private val privateTopProperty: Int = 5

// Test the creation of the public bridge function that will wrap the call to this private function.
// After inlining, the call to this function will be replaced by the call to the public bridge and
// a call to clinit() will be inserted in the bridge.
private fun topPrivateFun(): Int {
  assertTrue(privateTopProperty == 5)
  return privateTopProperty
}

class ClassWithInlineFun(var e: Int) {
  internal inline fun inlineFun(action: (Int) -> Int): Int {
    return action(e) + topPrivateFun()
  }
}
