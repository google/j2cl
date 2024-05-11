/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javaemul.internal;

import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/** Backend-specific utils for Throwable. */
public final class ThrowableUtils {

  /** JavaScript top type. */
  @JsType(isNative = true, name = "?", namespace = JsPackage.GLOBAL)
  public interface JsObject {
    String toString();
  }

  /** Gets the Java {@link Throwable} of the specified js {@code Error}. */
  public static Throwable getJavaThrowable(JsObject e) {
    return internalize(getJavaThrowableImpl(e));
  }

  @Wasm("extern.internalize")
  public static native <T> T internalize(WasmExtern t);

  /** Sets the Java {@link Throwable} of the specified js {@code Error}. */
  public static void setJavaThrowable(JsObject e, Throwable javaThrowable) {
    setJavaThrowableImpl(e, externalize(javaThrowable));
  }

  @Wasm("extern.externalize")
  public static native WasmExtern externalize(Throwable t);

  @JsMethod(name = "setJavaThrowable", namespace = "j2wasm.ExceptionUtils")
  private static native void setJavaThrowableImpl(JsObject error, WasmExtern javaThrowable);

  @JsMethod(name = "getJavaThrowable", namespace = "j2wasm.ExceptionUtils")
  private static native WasmExtern getJavaThrowableImpl(JsObject error);

  public static boolean isError(JsObject error) {
    return false;
  }

  /** JavaScript {@code Error}. Placeholder in Wasm. */
  @JsType(isNative = true, name = "Error", namespace = JsPackage.GLOBAL)
  public static class NativeError implements JsObject {
    public static boolean hasCaptureStackTraceProperty;

    @JsOverlay
    public static void captureStackTrace(NativeError error) {
      // No op to avoid importing the function which breaks Firefox.
    }

    public String stack;
  }

  public static boolean isTypeError(JsObject error) {
    return false;
  }

  /** JavaScript {@code TypeError}. Placeholder in Wasm. */
  public static class NativeTypeError {}

  private ThrowableUtils() {}
}
