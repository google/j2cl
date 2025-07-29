/*
 * Copyright 2018 Google Inc.
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
import kotlin.jvm.JvmStatic

/** Utility methods to test for the presence of properties in objects. */
object PropertyUtils {
  @JsMethod @JvmStatic external fun hasPublicMethod(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasPublicSubclassMethod(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasPublicStaticSubclassMethod(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasPrivateSubclassMethod(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasProtectedSubclassMethod(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasPackageSubclassMethod(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasPublicSubclassField(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasPublicStaticSubclassField(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasPrivateSubclassField(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasProtectedSubclassField(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasPackageSubclassField(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasPublicFinalField(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasPrivateStaticFinalField(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasProtectedStaticFinalField(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasDefaultStaticFinalField(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasProtectedStaticMethod(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasPrivateStaticMethod(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasDefaultStaticMethod(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasOwnPropertyMine(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasOwnPropertyToString(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasNotExported_1(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasNotExported_2(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasNOT_EXPORTED_1(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasNOT_EXPORTED_2(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasNOT_EXPORTED_3(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasNOT_EXPORTED_4(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun hasNOT_EXPORTED_5(obj: Any?): Boolean

  @JsMethod @JvmStatic external fun toCtor(obj: Class<*>): Any
}
