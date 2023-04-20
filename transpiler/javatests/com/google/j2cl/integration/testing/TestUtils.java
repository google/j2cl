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
package com.google.j2cl.integration.testing;

/** Utility functions needed by integration tests. */
public final class TestUtils {
  public static boolean isJvm() {
    return System.getProperty("java.version", null) != null;
  }

  public static boolean isWasm() {
    return Platform.IS_WASM;
  }

  public static boolean isJavaScript() {
    // TODO(b/278942389): should return true for J2KT-JS.
    return !isJvm() && !isWasm() && !isJ2Kt();
  }

  public static boolean isJ2Kt() {
    return Platform.IS_J2KT;
  }

  @SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
  public static <T> T getUndefined() {
    // Arrays of references are not explicitly initialized in J2CL. For JavaScript those will
    // be filled with undefined values. For other platforms, they will be filled with nulls.
    return (T) (new Object[1])[0];
  }

  private TestUtils() {}
}
