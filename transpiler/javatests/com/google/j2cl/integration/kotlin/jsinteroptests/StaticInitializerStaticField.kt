/*
 * Copyright 2014 Google Inc.
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

import jsinterop.annotations.JsIgnore
import jsinterop.annotations.JsType

/** Test access to static field from JS, ensuring clinit run. */
@JsType
class StaticInitializerStaticField {
  companion object {
    @JvmField val EXPORTED_1 = Any()

    // Not final
    @JvmField var EXPORTED_2 = Any()

    @JvmField @JsIgnore val NOT_EXPORTED_1 = Any()

    // Not public
    @JvmField internal val NOT_EXPORTED_3 = Any()

    // Not public
    @JvmStatic private val NOT_EXPORTED_4 = Any()

    // Not public
    @JvmField protected val NOT_EXPORTED_5 = Any()
  }

  // Not static
  val NOT_EXPORTED_2 = Any()

  /** Test interface that export a static field. */
  @JsType
  interface InterfaceWithField {
    companion object {
      @JvmField val STATIC = Any()
    }
  }
}
