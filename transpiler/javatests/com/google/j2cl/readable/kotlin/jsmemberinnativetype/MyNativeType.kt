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
package jsmemberinnativetype

import jsinterop.annotations.JsOverlay
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType
import kotlin.js.definedExternally

@JsType(isNative = true, namespace = "test.foo")
class MyNativeType {
  companion object {
    @JvmField var staticField: Int = definedExternally

    @JvmStatic @JsProperty external fun getStaticField(): Int

    @JvmStatic @JsProperty external fun setStaticField(value: Int)
  }

  @JvmField var publicField: Int = definedExternally

  private var privateField: Int = definedExternally

  @JvmField internal var packageField: Int = definedExternally

  @JvmField protected var protectedField: Int = definedExternally

  @JsProperty external fun getPublicField(): Int

  @JsProperty external fun setPublicField(value: Int)

  external fun publicMethod()

  private external fun privateMethod()

  internal external fun packageMethod()

  protected external fun protectedMethod()

  @JsOverlay
  fun useFieldsAndMethods() {
    val jsProperties = publicField + privateField + packageField + protectedField + staticField

    val jsPropertyMethods = getPublicField() + getStaticField()
    setPublicField(1)
    setStaticField(2)

    publicMethod()
    privateMethod()
    packageMethod()
    protectedMethod()
  }
}
