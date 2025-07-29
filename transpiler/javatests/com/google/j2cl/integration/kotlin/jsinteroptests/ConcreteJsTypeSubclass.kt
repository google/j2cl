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

import jsinterop.annotations.JsConstructor

/** This concrete test class is not annotated as a @JsType but its parent is. */
class ConcreteJsTypeSubclass @JsConstructor constructor() : ConcreteJsType() {
  /** Overrides an exported method in the parent class and so will also itself be exported. */
  override fun publicMethod(): Int = 20

  /*
   * The following members will not be exported since this class is not an @JsType and these members
   * do not override already exported members.
   */

  fun publicSubclassMethod(): Int = super.publicMethod()

  private fun privateSubclassMethod() {}

  protected fun protectedSubclassMethod() {}

  internal fun packageSubclassMethod() {}

  @JvmField val publicSubclassField: Int = 20

  private val privateSubclassField: Int = 20

  @JvmField protected val protectedSubclassField: Int = 20

  @JvmField internal val packageSubclassField: Int = 20

  companion object {
    @JvmStatic fun publicStaticSubclassMethod() {}

    @JvmField val publicStaticSubclassField: Int = 20
  }
}
