/*
 * Copyright 2024 Google Inc.
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
package nativejstypes

import jsinterop.annotations.JsType
import kotlin.js.definedExternally

@JsType(isNative = true, namespace = "", name = "toplevel")
class TopLevel {

  companion object {
    @JvmField var x: Int = definedExternally
  }

  @JsType(isNative = true)
  class Nested {
    @JvmField var x: Int = definedExternally
  }
}

@JsType(isNative = true, namespace = "", name = "toplevel.Nested")
class TopLevelNestedReference {
  @JvmField var x: Int = definedExternally
}
