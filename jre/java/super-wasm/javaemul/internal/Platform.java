/*
 * Copyright 2021 Google Inc.
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

import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;

/** Platform specific utilities with Wasm specific implementation. */
public final class Platform {

  @SuppressWarnings("SelfEquality")
  public static boolean isNaN(double x) {
    return x != x;
  }

  @SuppressWarnings("SelfEquality")
  public static boolean isFinite(double x) {
    return (x - x) == 0;
  }

  @Wasm("i64.reinterpret_f64")
  public static native long doubleToRawLongBits(double value);

  @Wasm("f64.reinterpret_i64")
  public static native double longBitsToDouble(long value);

  @Wasm("i32.reinterpret_f32")
  public static native int floatToRawIntBits(float value);

  @Wasm("f32.reinterpret_i32")
  public static native float intBitsToFloat(int value);

  public static String toPrecision(double value, int precision) {
    return String.fromJsString(nativeToPrecision(value, precision));
  }

  @JsMethod(name = "Number.toPrecision", namespace = JsPackage.GLOBAL)
  private static native String.NativeString nativeToPrecision(double value, int precision);

  public static boolean objectsStringEquals(String x, String y) {
    return (x == y) || (x != null && x.equals(y));
  }

  public static boolean isEqual(Boolean x, Object y) {
    return y instanceof Boolean && x.booleanValue() == ((Boolean) y).booleanValue();
  }

  public static boolean isEqual(Double x, Object y) {
    return y instanceof Double && Double.doubleToLongBits(x) == Double.doubleToLongBits((Double) y);
  }

  public static boolean isEqual(Float x, Object y) {
    return y instanceof Float && Float.floatToIntBits(x) == Float.floatToIntBits((Float) y);
  }

  public static int hashCode(double x) {
    return Long.hashCode(Double.doubleToLongBits(x));
  }

  public static int hashCode(float x) {
    return Float.floatToIntBits(x);
  }

  @Wasm("ref.is_null")
  public static native boolean isNull(Object o);

  private Platform() {}
}
