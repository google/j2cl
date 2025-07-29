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
package jsignore

import jsinterop.annotations.JsIgnore
import jsinterop.annotations.JsType

@JsType
class JsIgnoreExample {
  var exportedField: Int = 10
  @JsIgnore var notExportedField: Int = 20
  val CONSTNAME: Any = Object()

  val exportedGetterProperty: Int
    get() = 30

  val notExportedGetterProperty: Int
    @JsIgnore get() = 30

  var exportedGetterSetterProperty: Int = 40
    get() = field + 1
    set(value) {
      field = value - 1
    }

  @JsIgnore
  var notExportedGetterSetterProperty: Int = 50
    get() = field + 1
    set(value) {
      field = value - 1
    }

  var getterOnlyExposedProperty: Int = 60
    get() = field + 1
    @JsIgnore
    set(value) {
      field = value - 1
    }

  var setterOnlyExposedProperty: Int = 70
    @JsIgnore get() = field + 1
    set(value) {
      field = value - 1
    }

  fun exportedFunction() {}

  @JsIgnore fun notExportedFunction() {}
}
