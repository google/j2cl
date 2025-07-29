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
package removeunusedcontructorprops

import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod

private var flag = false

class Inner {
  /** This will ensure that we go through the setter for the static field. */
  companion object {
    fun raiseFlag() {
      flag = true
    }
  }
}

/**
 * This is test is intended to fail if compiled with "--remove_unused_constructor_properties=ON".
 */
fun main(vararg unused: String) {
  Inner.raiseFlag()
  assertTrue(getFlag() == true)
}

/** A native implementation that grab the underlying flag value directly. */
@JsMethod external fun getFlag(): Boolean
