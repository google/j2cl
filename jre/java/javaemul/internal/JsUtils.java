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
import jsinterop.annotations.JsProperty;

/** Provides an interface for simple JavaScript idioms that can not be expressed in Java. */
public final class JsUtils {

  @JsMethod(namespace = JsPackage.GLOBAL, name = "typeof")
  public static native String typeOf(Object obj);

  @JsMethod
  public static native boolean isUndefined(@DoNotAutobox Object value);

  @JsProperty(namespace = JsPackage.GLOBAL, name = "undefined")
  public static native Object undefined();

  @UncheckedCast
  public static <T> T coerceToNull(@DoNotAutobox T value) {
    return isUndefined(value) ? null : value;
  }

  @JsMethod
  @UncheckedCast
  public static native <T> T uncheckedCast(@DoNotAutobox Object o);

  @JsMethod
  public static native int coerceToInt(Double o);

  @JsMethod
  @UncheckedCast
  public static native <T> T getProperty(Object map, String key);

  @JsMethod
  @UncheckedCast
  public static native void setProperty(Object map, String key, Object value);

  private JsUtils() {}
}
