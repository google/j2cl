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

/** Backend-specific utils for Throwable. */
public final class ThrowableUtils {

  /** Gets the Java {@link Throwable} of the specified js {@code Error}. */
  public static Throwable getJavaThrowable(Object e) {
    // Wasm doesn't yet support conversion from JS errors.
    throw new UnsupportedOperationException();
  }

  /** Sets the Java {@link Throwable} of the specified js {@code Error}. */
  public static void setJavaThrowable(Object error, Throwable javaThrowable) {
    // We are currently not linking the error back so this is no-op.
    // In the future if JS errors become accessible from Wasm, we should reconsider this.
  }

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
