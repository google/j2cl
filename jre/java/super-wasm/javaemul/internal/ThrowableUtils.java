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

import javaemul.internal.Exceptions.JsErrorWrapper;
import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;

/** Backend-specific utils for Throwable. */
public final class ThrowableUtils {

  /** Gets the Java {@link Throwable} of the specified js {@code Error}. */
  public static Throwable getJavaThrowable(Object e) {
    return internalize(getJavaThrowableImpl(((JsErrorWrapper) e).error));
  }

  @Wasm("extern.internalize")
  public static native <T> T internalize(WasmExtern t);

  /** Sets the Java {@link Throwable} of the specified js {@code Error}. */
  public static void setJavaThrowable(Object e, Throwable javaThrowable) {
    setJavaThrowableImpl(((JsErrorWrapper) e).error, externalize(javaThrowable));
  }

  @Wasm("extern.externalize")
  public static native WasmExtern externalize(Throwable t);

  @JsMethod(name = "setJavaThrowable", namespace = "j2wasm.ExceptionUtils")
  private static native void setJavaThrowableImpl(WasmExtern error, WasmExtern javaThrowable);

  @JsMethod(name = "getJavaThrowable", namespace = "j2wasm.ExceptionUtils")
  private static native WasmExtern getJavaThrowableImpl(WasmExtern error);

  /** JavaScript {@code Error}. Placeholder in Wasm. */
  public static class NativeError {
    public static boolean hasCaptureStackTraceProperty;

    public static void captureStackTrace(Object error) {}

    public String stack;
  }

  /** JavaScript {@code TypeError}. Placeholder in Wasm. */
  public static class NativeTypeError {}

  private ThrowableUtils() {}
}
