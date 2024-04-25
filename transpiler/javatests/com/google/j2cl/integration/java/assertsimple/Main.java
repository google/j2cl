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
package assertsimple;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.fail;
import static com.google.j2cl.integration.testing.AssertsBase.assertTrue;
import static com.google.j2cl.integration.testing.TestUtils.isJvm;

/** Test method body, assert statement, and binary expression with number literals work fine. */
@SuppressWarnings("ComplexBooleanConstant") // literal boolean expressions
public class Main {
  public static void main(String... args) {
    testAssert_succeeds();
    testAssert_fails_differentMessageExpressionTypes();
    // TODO(b/202076599): Remove conditional once bug is fixed.
    if (isJvm()) {
      testAssert_sideEffects();
    }
  }

  private static void testAssert_succeeds() {
    assert 1 + 2 == 3;
  }

  private static void testAssert_fails_differentMessageExpressionTypes() {
    try {
      assert 2 == 3;
      fail("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success. In Kotlin, exception message is never null and defaults to "Assertion failed".
      String message = expected.getMessage();
      assertTrue(message == null || message.equals("Assertion failed"));
    }

    try {
      assert 2 == 3 : getDescription();
      fail("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assertEquals("custom message", expected.getMessage());
    }

    try {
      assert 2 == 3 : new RuntimeException("42");
      fail("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assertEquals("42", expected.getCause().getMessage());
    }

    try {
      assert 2 == 3 : 42;
      fail("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assertEquals("42", expected.getMessage());
    }

    try {
      assert 2 == 3 : 42L;
      fail("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assertEquals("42", expected.getMessage());
    }

    try {
      assert 2 == 3 : true;
      fail("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assertEquals("true", expected.getMessage());
    }

    try {
      assert 2 == 3 : null;
      fail("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assertEquals("null", expected.getMessage());
    }

    try {
      assert 2 == 3 : 'g';
      fail("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assertEquals("g", expected.getMessage());
    }
  }

  private static Object getDescription() {
    return new Object() {
      @Override
      public String toString() {
        return "custom message";
      }
    };
  }

  private static void testAssert_sideEffects() {

    assertEquals(0, sideEffects);
    assert 1 + 2 == 3 : getMessageWithSideEffects();
    assertEquals(0, sideEffects);

    try {
      assert 2 == 3 : getMessageWithSideEffects();
      fail("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assertEquals(1, sideEffects);
    }
  }

  private static int sideEffects = 0;

  private static String getMessageWithSideEffects() {
    sideEffects++;
    return "message";
  }
}
