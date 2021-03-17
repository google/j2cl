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
 * This class introduces an indirection for assertThrows methods so we can super source them and
 * make them noop for WASM.
 */
public class AssertsBase {
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

  // DO NOT INTRODUCE a helper from a Supplier, since the implicit return in the lambda might
  // introduce casts and autoboxing. The whole purpose of this assert helper is to make sure that
  // the code written by the user does not omit casts.
  public static void assertThrowsClassCastException(JsRunnable runnable) {
    assertThrowsClassCastException(runnable, (String) null);
  }

  public static void assertThrowsClassCastException(
      JsRunnable runnable, String qualifiedBinaryName) {
    try {
      runnable.run();
      fail("Should have thrown ClassCastException");
    } catch (ClassCastException expected) {
      if (qualifiedBinaryName != null) {
        assertTrue(
            "Got unexpected message " + expected.getMessage(),
            expected.getMessage().endsWith("cannot be cast to " + qualifiedBinaryName));
      }
    }
  }

  public static void assertThrowsClassCastException(JsRunnable runnable, Class<?> toClass) {
    assertThrowsClassCastException(runnable, toClass.getName());
  }

  public static void assertThrowsNullPointerException(JsRunnable runnable) {
    assertThrows(NullPointerException.class, runnable);
  }

  public static void assertThrows(Class<? extends Exception> exceptionClass, JsRunnable runnable) {
    try {
      runnable.run();
    } catch (Throwable e) {
      if (e.getClass() == exceptionClass) {
        return;
      }
    }
    fail("Should have thrown " + exceptionClass);
  }

  static String getFailureMessage(Object expected, Object actual, String msg) {
    String expectedString = expected == null ? null : expected.toString();
    String actualString = actual == null ? null : actual.toString();
    return "<" + actualString + "> " + msg + " <" + expectedString + ">";
  }
}
