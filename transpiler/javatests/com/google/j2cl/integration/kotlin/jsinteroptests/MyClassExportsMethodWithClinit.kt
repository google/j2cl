/*
 * Copyright 2014 Google Inc.
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
package jsinteroptests

import com.google.j2cl.integration.testing.Asserts.assertEquals
import jsinterop.annotations.JsConstructor
import kotlin.random.Random

internal class MyClassExportsMethodWithClinit {
  companion object {
    @JvmField var magicNumber = 0

    init {
      // prevent optimizations from inlining this clinit()
      if (Random.Default.nextDouble() > -1.0) {
        magicNumber = 42
      }
    }
  }

  @JsConstructor
  constructor() {
    // ensure clinit() is called even when invoked from JS
    assertEquals(42, magicNumber)
    magicNumber = 23
  }
}
