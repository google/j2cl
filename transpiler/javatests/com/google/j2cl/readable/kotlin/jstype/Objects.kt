/*
 * Copyright 2025 Google Inc.
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
package jstype

import jsinterop.annotations.JsType

@JsType
object SomeObject {
  var publicField: Int = 0
  private var privateField: Int = 0
  internal var packageField: Int = 0
  const val publicConst = 0

  fun publicMethod() {}

  private fun privateMethod() {}

  internal fun packageMethod() {}
}

interface NonJsTypeInterface {
  fun foo()
}

@JsType
interface JsTypeInterface {
  fun foo()

  fun bar()
}

@JsType
object JsTypeObjectImplementsNonJsType : NonJsTypeInterface {
  override fun foo() {}
}

@JsType
object JsTypeObjectImplementJsType : JsTypeInterface {
  override fun foo() {}

  override fun bar() {}
}

object NonJsTypeObjectImplementsJsType : JsTypeInterface {
  override fun foo() {}

  override fun bar() {}
}

object NonJsTypeObjectImplementsJsTypeAndNonJsType : JsTypeInterface, NonJsTypeInterface {
  override fun foo() {}

  override fun bar() {}
}
