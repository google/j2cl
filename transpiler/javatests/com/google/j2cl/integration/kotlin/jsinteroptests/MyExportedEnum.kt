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

import jsinterop.annotations.JsType

/** This enum is annotated as @JsType. */
@JsType
enum class MyExportedEnum {
  TEST1,
  TEST2;

  companion object {
    @JvmStatic fun publicStaticMethod(): Int = 0

    @JvmField val publicStaticFinalField = 1

    // explicitly marked @JsProperty fields must be final
    // but ones that are in an exported class don't need to be final
    @JvmField val publicStaticField = 2

    @JvmStatic private val privateStaticFinalField = 4

    @JvmField protected val protectedStaticFinalField = 5

    @JvmField internal val defaultStaticFinalField = 6

    @JvmStatic protected fun protectedStaticMethod(): Int = 0

    @JvmStatic internal fun defaultStaticMethod(): Int = 0

    @JvmStatic private fun privateStaticMethod(): Int = 0
  }

  val publicFinalField = 3

  fun publicMethod(): Int = 0
}
