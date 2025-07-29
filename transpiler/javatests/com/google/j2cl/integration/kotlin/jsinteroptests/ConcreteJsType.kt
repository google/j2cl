/*
 * Copyright 2015 Google Inc.
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

import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsType

/** This concrete test class is *directly* annotated as a @JsType. */
@JsType
open class ConcreteJsType {
  open fun publicMethod(): Int = 10

  open fun publicMethodAlsoExposedAsNonJsMethod(): Int = 100

  private fun privateMethod() {}

  open protected fun protectedMethod() {}

  internal fun packageMethod() {}

  @JvmField val publicField: Int = 10

  private val privateField: Int = 10

  @JvmField protected val protectedField: Int = 10

  @JvmField internal val packageField: Int = 10

  interface A {
    fun x(): Int
  }

  class AImpl1 : A {
    override fun x(): Int = 42
  }

  class AImpl2 : A {
    override fun x(): Int = 101
  }

  @SuppressWarnings("unusable-by-js") @JvmField var notTypeTightenedField: A = AImpl1()

  companion object {
    @JvmStatic fun publicStaticMethod() {}

    @JvmField val publicStaticField: Int = 10

    @JsMethod @JvmStatic external fun hasPublicMethod(obj: Any?): Boolean

    @JsMethod @JvmStatic external fun hasPublicStaticMethod(obj: Any?): Boolean

    @JsMethod @JvmStatic external fun hasPrivateMethod(obj: Any?): Boolean

    @JsMethod @JvmStatic external fun hasProtectedMethod(obj: Any?): Boolean

    @JsMethod @JvmStatic external fun hasPackageMethod(obj: Any?): Boolean

    @JsMethod @JvmStatic external fun hasPublicField(obj: Any?): Boolean

    @JsMethod @JvmStatic external fun hasPublicStaticField(obj: Any?): Boolean

    @JsMethod @JvmStatic external fun hasPrivateField(obj: Any?): Boolean

    @JsMethod @JvmStatic external fun hasProtectedField(obj: Any?): Boolean

    @JsMethod @JvmStatic external fun hasPackageField(obj: Any?): Boolean
  }
}
