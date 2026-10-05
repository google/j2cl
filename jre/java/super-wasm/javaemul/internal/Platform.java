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

  public static int compareDouble(double x, double y) {
    if (x < y) {
      return -1;
    }
    if (x > y) {
      return 1;
    }

    // Use MAX_VALUE (which is avalid NaN) for NaN so it is always larger than any other number that
    // is not NaN.
    long xBits = isNaN(x) ? Long.MAX_VALUE : doubleToRawLongBits(x);
    long yBits = isNaN(y) ? Long.MAX_VALUE : doubleToRawLongBits(y);
    return Long.compare(xBits, yBits);
  }

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

  /**
   * Returns true if two potential JS values are equal. The compiler rewrites `==` where at least
   * one operand is native into calls to these methods, which compare both values as JS values.
   *
   * <p>Note that this makes the semantics of `==` consistent with JS identity rather than Java
   * identity for values that have a JS representation (e.g. a Java `String` compares equal to a JS
   * string with the same contents).
   */
  @JsMethod(namespace = "j2wasm.EqualityUtils")
  public static native boolean isSame(WasmExtern left, WasmExtern right);

  /**
   * See {@link #isSame(WasmExtern, WasmExtern)}.
   *
   * <p>The cast to `WasmExtern` converts the Java side to its JS representation (via
   * `Object.toJs`), which is preferred over converting the JS side via `Object.fromJs` since the
   * latter might allocate a new wrapper (e.g. `JsObject`).
   */
  public static boolean isSame(WasmExtern left, Object right) {
    return isSame(left, (WasmExtern) right);
  }

  /** See {@link #isSame(WasmExtern, Object)}. */
  public static boolean isSame(Object left, WasmExtern right) {
    return isSame((WasmExtern) left, right);
  }

  private Platform() {}
}
