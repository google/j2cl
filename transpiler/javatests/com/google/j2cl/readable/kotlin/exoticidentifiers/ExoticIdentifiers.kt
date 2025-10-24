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
package exoticidentifiers

import jsinterop.annotations.JsEnum
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

fun `foo$bar`() {}

fun `bar%buzz`() {}

fun `buzz!bar`() {}

fun `2times`() {}

fun `¯|_(ツ)_|¯`() {}

class `exotic cla$$ name!` {
  var `exotic var$$ name!` = 1
}

enum class `exotic enum$$ name!` {
  `exotic enum$$ entry 1!`,
  `exotic enum$$ entry 2!`,
}

@JsEnum(name = "RenamedEnumWithValidIdentifier")
enum class `exotic J$enum name!` {
  @JsProperty(name = "renamedJsEnumEntry1") `exotic J$enum entry 1!`,
  @JsProperty(name = "renamedJsEnumEntry2") `exotic J$enum entry 2!`,
}

@JsType(name = "RenamedClassWithValidIdentifier")
class `exotic class$$ that will be renamed`<`Exotic Type parameter`> {
  @JsProperty(name = "renamedPropertyWithValidIdentifier")
  var `exotic var$$ that will be renamed`: Int = 1

  @JsMethod(name = "renamedFunctionWithValidIdentifier")
  fun `exotic function$$ that will be renamed`(`exotic parameter`: String) =
    `exotic parameter`.length + `exotic var$$ that will be renamed`
}
