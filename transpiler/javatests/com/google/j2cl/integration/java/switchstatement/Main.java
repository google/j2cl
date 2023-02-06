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
package switchstatement;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;

import java.util.function.Supplier;

public class Main {
  public static void main(String... args) {
    testSwitchValues();
    testSwitchVariableDeclarations();
    testSwitchNull();
    testSwitchWithErasureCast();
    testCascades();
    testStringSwitch();
  }

  private static void testSwitchValues() {
    assertTrue(getStringValue("zero") == 1); // Cascade
    assertTrue(getStringValue("one") == 1);
    assertTrue(getStringValue("two") == 2);
    assertTrue(getStringValue("three") == 3); // Default

    assertTrue(getCharValue('0') == 1); // Cascade
    assertTrue(getCharValue('1') == 1);
    assertTrue(getCharValue('2') == 2);
    assertTrue(getCharValue('3') == 3); // Default

    assertTrue(getIntValue(0) == 1); // Cascade
    assertTrue(getIntValue(1) == 1);
    assertTrue(getIntValue(2) == 2);
    assertTrue(getIntValue(3) == 3); // Default

    assertTrue(getBoxedIntValue(new Integer(0)) == 1); // Cascade
    assertTrue(getBoxedIntValue(new Integer(1)) == 1);
    assertTrue(getBoxedIntValue(new Integer(2)) == 2);
    assertTrue(getBoxedIntValue(new Integer(3)) == 3); // Default

    assertTrue(getEnumValue(Numbers.ZERO) == 1); // Cascade
    assertTrue(getEnumValue(Numbers.ONE) == 1);
    assertTrue(getEnumValue(Numbers.TWO) == 2);
    assertTrue(getEnumValue(Numbers.THREE) == 3); // Default
  }

  private static int getStringValue(String stringValue) {
    switch (stringValue) {
      case "zero":
      case "one":
        return 1;
      case "two":
        return 2;
      default:
        return 3;
    }
  }

  private static int getCharValue(char charValue) {
    switch (charValue) {
      case '0':
      case '1':
        return 1;
      default:
        return 3;
      case '2':
        return 2;
    }
  }

  private static int getBoxedIntValue(Integer i) {
    switch (i) {
      case 0:
      case 1:
        return 1;
      case 2:
        // Keep this assertion to make sure jscompiler does not optimize calls to the method away.
        assertFalse(i != 2);
        return 2;
      default:
        return 3;
    }
  }

  private static int getIntValue(int intValue) {
    switch (intValue) {
      case 0:
      case 1:
        return 1;
      case 2:
        return 2;
      default:
        return 3;
    }
  }

  public enum Numbers {
    ZERO,
    ONE,
    TWO,
    THREE
  }

  private static int getEnumValue(Numbers numberValue) {
    switch (numberValue) {
      case ZERO:
      case ONE:
        return 1;
      case TWO:
        // Keep this assertion to make sure jscompiler does not optimize calls to the method away.
        assertFalse(numberValue != Numbers.TWO);
        return 2;
      default:
        return 3;
    }
  }

  @SuppressWarnings("unused")
  private static void testSwitchVariableDeclarations() {
    switch (3) {
      case 1:
        int i = 0, unassigned;
        int unassigned2;
        int j = 2, b = j + 1;
        break;
      case 3:
        i = 3;
        assertTrue(i == 3);
        return;
    }

    switch (5) {
      case 5:
        int i = 1;
        break;
    }
    fail();
  }

  private static void testSwitchNull() {
    assertThrowsNullPointerException(() -> getBoxedIntValue(null));
    assertThrowsNullPointerException(() -> getEnumValue(null));
    assertThrowsNullPointerException(() -> getStringValue(null));
  }

  private static void testSwitchWithErasureCast() {
    Supplier<Numbers> supplier = () -> Numbers.ONE;
    switch (supplier.get()) {
      case ONE:
        break;
      default:
        fail();
    }

    assertThrowsClassCastException(
        () -> {
          Supplier<Numbers> integerSupplier = (Supplier) () -> new Integer(1);
          switch (integerSupplier.get()) {
            default:
              fail();
          }
        });
  }

  private static void testCascades() {
    assertEquals(3, testCascade_allFallThrough(1));
    assertEquals(2, testCascade_allFallThrough(2));
    assertEquals(1, testCascade_allFallThrough(3));

    assertFalse(testCascade_emptyCase(1));
    assertTrue(testCascade_emptyCase(2));
    assertTrue(testCascade_emptyCase(3));

    assertFalse(testCascade_ifStatement(1));
    assertTrue(testCascade_ifStatement(2));
    assertTrue(testCascade_ifStatement(3));

    assertTrue(testCascade_breakInner(1));
    assertTrue(testCascade_breakInner(2));

    assertFalse(testCascade_breakOuter(1));
    assertTrue(testCascade_breakOuter(2));

    assertEquals(10, testDefaultNotLast_fallThroughCase(1));
    assertEquals(10, testDefaultNotLast_fallThroughCase(2));
    assertEquals(100, testDefaultNotLast_fallThroughCase(3));

    assertEquals(10, testDefaultNotLast_fallThroughDefault(1));
    assertEquals(100, testDefaultNotLast_fallThroughDefault(2));
    assertEquals(100, testDefaultNotLast_fallThroughDefault(3));
  }

  private static int testCascade_allFallThrough(int i) {
    int result = 0;
    switch (i) {
      case 1:
        result++;
        // fall through
      case 2:
        result++;
        // fall through
      default:
        result++;
    }
    return result;
  }

  private static boolean testCascade_emptyCase(int i) {
    boolean result = false;
    switch (i) {
      case 1:
        break;
      case 2:
      default:
        result = true;
    }
    return result;
  }

  private static boolean testCascade_ifStatement(int i) {
    boolean result = false;
    switch (i) {
      case 1:
      case 2:
        if (i == 1) {
          break;
        }
        // fall through
      default:
        result = true;
    }
    return result;
  }

  private static boolean testCascade_breakInner(int i) {
    boolean result = false;
    switch (i) {
      case 1:
        INNER:
        while (true) {
          break INNER;
        }
        // fall through
      default:
        result = true;
    }
    return result;
  }

  private static boolean testCascade_breakOuter(int i) {
    boolean result = false;
    OUTER:
    switch (i) {
      case 1:
        INNER:
        while (true) {
          break OUTER;
        }
      default:
        result = true;
    }
    return result;
  }

  private static int testDefaultNotLast_fallThroughCase(int i) {
    int result = 0;
    switch (i) {
      case 1:
        // fall through
      default:
        result += 10;
        break;
      case 3:
        result += 100;
        break;
    }
    return result;
  }

  private static int testDefaultNotLast_fallThroughDefault(int i) {
    int result = 0;
    switch (i) {
      case 1:
        result += 10;
        break;
      default:
        // fall through
      case 3:
        result += 100;
        break;
    }
    return result;
  }

  private static void testStringSwitch() {
    // Test that switch on strings uses .equals() semantics.
    assertTrue(getStringValue(new String(new char[] {'o', 'n', 'e'})) == 1);
  }
}
