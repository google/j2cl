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

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/** Backend-specific utils for Throwable. */
public final class ThrowableUtils {

  /** JavaScript top type. */
  @JsType(isNative = true, name = "*", namespace = JsPackage.GLOBAL)
  public interface JsObject {
    String toString();
  }

  /** Gets the Java {@link Throwable} of the specified js {@code Error}. */
  public static Throwable getJavaThrowable(Object e) {
    return ((HasJavaThrowable) e).getJavaThrowable();
  }

  /** Sets the Java {@link Throwable} of the specified js {@code Error}. */
  @JsMethod
  public static native void setJavaThrowable(JsObject error, Throwable javaThrowable);

  public static boolean isError(JsObject error) {
    return error instanceof NativeError;
  }

  /** JavaScript {@code Error}. */
  @JsType(isNative = true, name = "Error", namespace = JsPackage.GLOBAL)
  public static class NativeError implements JsObject {
    @JsProperty(name = "captureStackTrace")
    public static boolean hasCaptureStackTraceProperty;

    public static native void captureStackTrace(NativeError error);

    public String stack;
  }

  public static boolean isTypeError(JsObject error) {
    return error instanceof NativeTypeError;
  }

  /** JavaScript {@code TypeError}. */
  @JsType(isNative = true, name = "TypeError", namespace = JsPackage.GLOBAL)
  public static class NativeTypeError implements JsObject {}

  @SuppressWarnings("unusable-by-js")
  @JsType(isNative = true, name = "?", namespace = JsPackage.GLOBAL)
  private interface HasJavaThrowable {
    @JsProperty(name = "__java$exception")
    Throwable getJavaThrowable();
  }

  private ThrowableUtils() {}
}
