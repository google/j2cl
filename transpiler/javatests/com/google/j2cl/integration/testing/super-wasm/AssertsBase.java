/*
 * Copyright 2021 Google Inc.
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

import com.google.j2cl.integration.testing.Asserts.JsRunnable;

/**
 * Re-implement assertThrows methods as no-op operations because J2WASM does not have proper
 * exception emulation. This will allow us to enable some integration tests for Wasm that uses these
 * methods without modifying them directly.
 */
// TODO(b/183769034): Get rid of this class when exception support is completely implemented by
// J2WASM, including the ability to catch traps like divide by 0.
class AssertsBase {
  public static void fail() {
    throw new AssertionError();
  }

  public static void fail(String message) {
    throw new AssertionError(message);
  }

  public static void assertTrue(boolean condition) {
    if (!condition) {
      fail();
    }
  }

  public static void assertTrue(String message, boolean condition) {
    if (!condition) {
      fail(message);
    }
  }

  public static void assertThrowsClassCastException(JsRunnable runnable) {}

  public static void assertThrowsClassCastException(
      JsRunnable runnable, String qualifiedBinaryName) {}

  public static void assertThrows(Class<? extends Exception> exceptionClass, JsRunnable runnable) {}

  public static void assertThrowsClassCastException(JsRunnable runnable, Class<?> toClass) {}

  public static void assertThrowsNullPointerException(JsRunnable runnable) {}

  static String getFailureMessage(Object expected, Object actual, String msg) {
    return "<" + actual + "> " + msg + " <" + expected + ">";
  }
}
