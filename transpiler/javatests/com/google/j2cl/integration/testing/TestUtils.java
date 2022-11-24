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

import javaemul.internal.annotations.Wasm;

/** Utility functions needed by integration tests. */
public final class TestUtils {
  public static boolean isJvm() {
    return System.getProperty("java.version", null) != null;
  }

  @Wasm("i32.const 1")
  public static boolean isWasm() {
    return false;
  }

  public static boolean isJavaScript() {
    return !isJvm() && !isWasm();
  }

  @SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
  public static <T> T getUndefined() {
    // Arrays of references are not explicitly initialized in J2CL. For JavaScript those will
    // be filled with undefined values. For other platforms, they will be filled with nulls.
    return (T) (new Object[1])[0];
  }

  private TestUtils() {}
}
