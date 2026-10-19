/*
 * Copyright 2026 Google Inc.
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
package jstype

import jsinterop.annotations.JsType

@JsType
open class JsTypeFromOtherLibrary(private val value: Int) {
  // Its backing field is private, so it must not be seen as a JavaScript field clashing with the
  // getter when the subclass is compiled.
  val storedProperty: Int = value

  // It has no backing field, as its value is computed by its getter, so no JavaScript field must
  // be seen clashing with the getter when the subclass is compiled.
  val computedProperty: Int
    get() = value * 2
}
