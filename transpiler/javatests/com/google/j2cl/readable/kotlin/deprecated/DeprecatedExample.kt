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
package deprecated

import jsinterop.annotations.JsEnum
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsType

/** A set of an examples showing how @[Deprecated] annotations are generated in JavaScript. */
@Deprecated("") var DEPRECATED_STATIC_FIELD = "deprecated field"

@Deprecated("") fun deprecatedStaticMethod(someObject: Any?) {}

var fieldWithDeprecatedGetter = 0
  @Deprecated("") get() = 2

@JsType
@Deprecated("")
class DeprecatedExample @Deprecated("") constructor() {
  @Deprecated("") var deprecatedInstanceField: String? = "deprecated field"
  @Deprecated("") fun deprecatedInstanceMethod(someArg: String?) {}

  @JsType
  @Deprecated("")
  internal interface DeprecatedInterface {
    @Deprecated("") fun doAThing(anInt: Int)
  }

  @JsFunction
  @Deprecated("")
  internal fun interface DeprecatedJsFunction {
    @Deprecated("") fun doAThing(aThing: Any?)
  }

  @JsType
  @Deprecated("")
  internal enum class DeprecatedEnum {
    @Deprecated("") A_VALUE
  }

  @JsEnum
  @Deprecated("")
  internal enum class DeprecatedJsEnum {
    @Deprecated("") A_VALUE
  }
}
