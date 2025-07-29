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
package instancejsmethods

import jsinterop.annotations.JsMethod

open class Parent : SuperParent() {
  /** JsMethod that overrides a non-JsMethod with renaming. */
  @JsMethod(name = "sum") open override fun `fun`(a: Int, b: Int): Int = a + b

  /** JsMethod that overrides a non-JsMethod without renaming. */
  @JsMethod open override fun bar(a: Int, b: Int): Int = a * b

  /** A top level JsMethod. */
  @JsMethod(name = "myFoo") open fun foo(a: Int): Int = a
}
