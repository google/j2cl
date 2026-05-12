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
package jsinteroptests;

import jsinterop.annotations.JsType;

/** Utility methods to test for the presence of properties in objects. */
@JsType(isNative = true, namespace = "jsinteroptests")
public class PropertyUtils {
  public static native boolean hasPublicMethod(Object obj);

  public static native boolean hasPublicSubclassMethod(Object obj);

  public static native boolean hasPublicStaticSubclassMethod(Object obj);

  public static native boolean hasPrivateSubclassMethod(Object obj);

  public static native boolean hasProtectedSubclassMethod(Object obj);

  public static native boolean hasPackageSubclassMethod(Object obj);

  public static native boolean hasPublicSubclassField(Object obj);

  public static native boolean hasPublicStaticSubclassField(Object obj);

  public static native boolean hasPrivateSubclassField(Object obj);

  public static native boolean hasProtectedSubclassField(Object obj);

  public static native boolean hasPackageSubclassField(Object obj);

  public static native boolean hasPublicFinalField(Object obj);

  public static native boolean hasPrivateStaticFinalField(Object obj);

  public static native boolean hasProtectedStaticFinalField(Object obj);

  public static native boolean hasDefaultStaticFinalField(Object obj);

  public static native boolean hasProtectedStaticMethod(Object obj);

  public static native boolean hasPrivateStaticMethod(Object obj);

  public static native boolean hasDefaultStaticMethod(Object obj);

  public static native boolean hasOwnPropertyMine(Object obj);

  public static native boolean hasOwnPropertyToString(Object obj);

  public static native boolean hasNotExported_1(Object obj);

  public static native boolean hasNotExported_2(Object obj);

  public static native boolean hasNOT_EXPORTED_1(Object obj);

  public static native boolean hasNOT_EXPORTED_2(Object obj);

  public static native boolean hasNOT_EXPORTED_3(Object obj);

  public static native boolean hasNOT_EXPORTED_4(Object obj);

  public static native boolean hasNOT_EXPORTED_5(Object obj);
}
