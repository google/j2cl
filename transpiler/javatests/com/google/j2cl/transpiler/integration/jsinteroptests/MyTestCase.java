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
package com.google.j2cl.transpiler.integration.jsinteroptests;

import java.util.Arrays;

public class MyTestCase {
  public static void assertEquals(Object expected, Object actual) {
    assert expected.equals(actual)
        : "Not equals - expected: <" + expected + "> - actual: <" + actual + ">";
  }

  public static void assertEquals(Object[] expected, Object[] actual) {
    String message =
        "Not equals - expected: <"
            + Arrays.toString(expected)
            + "> - actual: <"
            + Arrays.toString(actual)
            + ">";
    assert expected.length == actual.length : message;

    for (int i = 0; i < expected.length; i++) {
      assert expected[i].equals(actual[i]) : message;
    }
  }

  public static void assertEquals(int expected, int actual) {
    assert (expected == actual);
  }

  public static void assertTrue(boolean condition) {
    assert condition;
  }

  public static void assertFalse(boolean condition) {
    assert !condition;
  }

  public static void assertTrue(String message, boolean condition) {
    assert condition : message;
  }

  public static void assertFalse(String message, boolean condition) {
    assert !condition : message;
  }

  public static void assertNotNull(Object object) {
    assert object != null;
  }

  public static void assertNull(Object object) {
    assert object == null;
  }

  public static void assertSame(Object expected, Object actual) {
    assert expected == actual
        : "Not same - expected: <" + expected + "> - actual: <" + actual + ">";
  }

  public static void fail(String message) {
    assert false : message;
  }
}
