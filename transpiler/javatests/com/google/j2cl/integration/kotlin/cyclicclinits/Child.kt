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

open class Parent {
  companion object {
    init {
      // Parent.$clinit() executes before Child.$clinit(), so fieldInChild has not been initialized.
      assertTrue(Child.fieldInChild == null)
    }
  }
}

class Child : Parent() {
  companion object {
    @JvmField var fieldInChild: Any = 1

    fun test() {
      assertTrue(fieldInChild != null)
    }
  }
}
