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
package javaemul.internal;

import javaemul.internal.annotations.DoNotAutobox;
import javaemul.internal.annotations.UncheckedCast;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/** Provides an interface for simple JavaScript idioms that can not be expressed in Java. */
@SuppressWarnings("unusable-by-js")
public final class JsUtils {

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Date.now")
  public static native double getTime();

  @JsMethod(namespace = JsPackage.GLOBAL, name = "performance.now")
  public static native double performanceNow();

  @JsMethod(namespace = JsPackage.GLOBAL)
  public static native int parseInt(String s, int radix);

  @JsMethod(namespace = JsPackage.GLOBAL)
  public static native double parseFloat(String str);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "typeof")
  public static native String typeOf(Object obj);

  public static String intToString(int value, int radix) {
    return numberToString(value, radix);
  }

  public static String uintToString(int value, int radix) {
    return numberToString(toDoubleFromUnsignedInt(value), radix);
  }

  @JsMethod
  public static native int toDoubleFromUnsignedInt(int value);

  private static String numberToString(double value, int radix) {
    NativeNumber number = JsUtils.uncheckedCast(value);
    return number.toString(radix);
  }

  @JsType(isNative = true, name = "Number", namespace = JsPackage.GLOBAL)
  private interface NativeNumber {
    String toString(int radix);
  }

  @JsMethod
  public static native boolean isUndefined(Object value);

  @JsMethod
  @UncheckedCast
  public static native <T> T uncheckedCast(@DoNotAutobox Object o);

  @JsMethod
  @UncheckedCast
  public static native <T> T getProperty(Object map, String key);

  private JsUtils() {}
}

