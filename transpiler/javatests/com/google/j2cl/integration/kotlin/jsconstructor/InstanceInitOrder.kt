/*
 * Copyright 2017 Google Inc.
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
package jsconstructor

import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsConstructor

class InstanceInitOrder {
  companion object {
    @JvmStatic var initStep: Int = 1

    @JvmStatic
    fun test() {
      val m = InstanceInitOrder()
      assertTrue(initStep == 6)
    }
  }

  var field1: Int = this.initializeField1()
  var field2: Int = initializeField2()

  init {
    assertTrue(initStep++ == 3) // #3
  }

  @JsConstructor
  constructor() {
    assertTrue(initStep++ == 5) // #5
  }

  init {
    assertTrue(initStep++ == 4) // #4
  }

  fun initializeField1(): Int {
    assertTrue(initStep++ == 1) // #1
    return 0
  }

  fun initializeField2(): Int {
    assertTrue(initStep++ == 2) // #2
    return 0
  }
}
