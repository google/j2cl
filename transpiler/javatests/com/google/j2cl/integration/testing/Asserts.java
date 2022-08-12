/*
 * Copyright 2017 Google Inc.
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

import javaemul.internal.annotations.DoNotAutobox;
import jsinterop.annotations.JsFunction;

public class Asserts extends AssertsBase {

  public static void assertFalse(boolean condition) {
    assertTrue(!condition);
  }

  public static void assertFalse(String message, boolean condition) {
    assertTrue(message, !condition);
  }

  public static void assertNull(Object object) {
    assertSame(null, object);
  }

  public static void assertNotNull(Object object) {
    assertNotSame(null, object);
  }

  public static void assertEquals(@DoNotAutobox Object expected, @DoNotAutobox Object actual) {
    assertEquals(getEqualsDefaultFailureMessage(expected, actual), expected, actual);
  }

  public static void assertEquals(
      String message, @DoNotAutobox Object expected, @DoNotAutobox Object actual) {
    assertTrue(message, expected == null ? actual == null : expected.equals(actual));
  }

  public static void assertEquals(Object[] expected, Object[] actual) {
    assertEquals(
        getFailureMessage(expected, actual, "should have the same length as"),
        expected.length,
        actual.length);

    for (int i = 0; i < expected.length; i++) {
      assertEquals(
          "Mismatch at array element ["
              + i
              + "]:"
              + getEqualsDefaultFailureMessage(expected, actual),
          expected[i],
          actual[i]);
    }
  }

  public static void assertEquals(byte[] expected, byte[] actual) {
    assertEquals(
        getFailureMessage(expected, actual, "should have the same length as"),
        expected.length,
        actual.length);

    for (int i = 0; i < expected.length; i++) {
      assertEquals(
          "Mismatch at array element ["
              + i
              + "]:"
              + getEqualsDefaultFailureMessage(expected, actual),
          expected[i],
          actual[i]);
    }
  }

  public static void assertEqualsDelta(double expected, double actual, double delta) {
    if (Double.compare(expected, actual) == 0) {
      return;
    }

    if ((Math.abs(expected - actual) <= delta)) {
      return;
    }

    fail("Actual: " + actual);
  }

  public static void assertEqualsDelta(float expected, float actual, float delta) {
    if (Float.compare(expected, actual) == 0) {
      return;
    }

    if ((Math.abs(expected - actual) <= delta)) {
      return;
    }

    fail("Actual: " + actual);
  }

  public static void assertNotEquals(@DoNotAutobox Object expected, @DoNotAutobox Object actual) {
    assertNotEquals(getNotEqualsDefaultFailureMessage(expected, actual), expected, actual);
  }

  public static void assertNotEquals(
      String message, @DoNotAutobox Object expected, @DoNotAutobox Object actual) {
    assertFalse(message, expected == null ? actual == null : expected.equals(actual));
  }

  public static void assertSame(Object expected, Object actual) {
    assertTrue(getFailureMessage(expected, actual, "should be the same as"), expected == actual);
  }

  public static void assertNotSame(Object expected, Object actual) {
    assertFalse(
        getFailureMessage(expected, actual, "should not be the same as"), expected == actual);
  }

  @JsFunction
  public interface JsRunnable {
    void run();
  }

  /**
   * Checks that the actual runtime type of {@code actual} is {@code expectedType}.
   *
   * <p>Note that {@code actual} will not be autoboxed if it is a primitive or JsEnum.
   */
  public static void assertUnderlyingTypeEquals(
      Class<?> expectedType, @DoNotAutobox Object actual) {
    assertTrue(
        getFailureMessage(
            expectedType.getCanonicalName(),
            actual.getClass().getCanonicalName(),
            "should be the same as"),
        expectedType == actual.getClass());
  }

  private static String getEqualsDefaultFailureMessage(Object expected, Object actual) {
    return getFailureMessage(expected, actual, "should be equal to");
  }

  private static String getNotEqualsDefaultFailureMessage(Object expected, Object actual) {
    return getFailureMessage(expected, actual, "should not be equal to");
  }
  
  public static void assertThrowsArithmeticException(JsRunnable runnable) {
    assertThrows(ArithmeticException.class, runnable);
  }

  public static void assertThrowsArrayStoreException(JsRunnable runnable) {
    assertThrows(ArrayStoreException.class, runnable);
  }
}
