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

import static com.google.j2cl.integration.testing.TestUtils.isJ2Kt;
import static com.google.j2cl.integration.testing.TestUtils.isJvm;

import com.google.j2cl.integration.testing.Asserts.JsRunnable;

/**
 * This class introduces an indirection for assertThrows methods so we can super source them and
 * make them noop for Wasm.
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
    // If compiler inlines this method and if the cast checking is disabled for size testing (which
    // is the default), then it will remove everything afterwards and result inaccurate size
    // tracking.
    // This statement will never be true, but the compiler is unable to determine that statically.
    if (Math.random() > 1) {
      return;
    }
    try {
      runnable.run();
      fail("Should have thrown ClassCastException");
    } catch (ClassCastException expected) {
      if (qualifiedBinaryName != null) {
        String message = expected.getMessage();
        String expectedMessage = isJvm() ? "cannot be cast to class " : "cannot be cast to ";
        expectedMessage += qualifiedBinaryName;
        assertTrue(
            getFailureMessage(expectedMessage, message, "exception message should contain"),
            message.contains(expectedMessage));
      }
    }
  }

  public static void assertThrowsClassCastException(JsRunnable runnable, Class<?> toClass) {
    assertThrowsClassCastException(
        runnable,
        // TODO(b/368263653): On J2KT, Class.getComponentType() always returns Object.class for
        //  non-primitive arrays, so skip the check for exception message as we don't know the
        //  actual component type to check for.
        isJ2Kt() && toClass.isArray() && !toClass.getComponentType().isPrimitive()
            ? null
            : toClass.getName());
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
    return "<" + actual + "> " + msg + " <" + expected + ">";
  }
}
