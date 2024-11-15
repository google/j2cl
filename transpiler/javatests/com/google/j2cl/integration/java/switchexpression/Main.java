/*
 * Copyright 2024 Google Inc.
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
package switchexpression;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;

import com.google.j2cl.integration.testing.TestUtils;
import java.util.function.Supplier;
import javaemul.internal.annotations.DoNotAutobox;

public class Main {
  public static void main(String... args) {
    testSwitchValues();
    testDefaultNotLast();
    testSwitchSideEffectInEnclosingScope();
    testSwitchNull();
    testSwitchWithErasureCast();
    testStringSwitch();
    testThrow();
    testAutoboxing();
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
    return switch (stringValue) {
      case "zero", "one" -> 1;
      case "two" -> 2;
      default -> 3;
    };
  }

  private static int getCharValue(char charValue) {
    return switch (charValue) {
      case '0', '1' -> 1;
      case '2' -> 2;
      default -> 3;
    };
  }

  private static int getBoxedIntValue(Integer i) {
    return switch (i) {
      case 0, 1 -> 1;
      case 2 -> {
        yield 2;
      }
      default -> 3;
    };
  }

  private static int getIntValue(int intValue) {
    return switch (intValue) {
      case 0, 1 -> 1;
      case 2 -> 2;
      default -> 3;
    };
  }

  public enum Numbers {
    ZERO,
    ONE,
    TWO,
    THREE
  }

  private static int getEnumValue(Numbers numberValue) {
    return switch (numberValue) {
      case ZERO, ONE -> 1;
      case TWO -> {
        yield 2;
      }
      default -> 3;
    };
  }

  private static void testDefaultNotLast() {
    int value = 3;
    String result =
        switch (value) {
          case 1, 2 -> "case 1, 2";
          default -> "default";
          case 3 -> "case 3";
        };
    assertEquals("case 3", result);
  }

  private static void testSwitchSideEffectInEnclosingScope() {
    int value = 0;
    int unused =
        switch (3) {
          case 1 -> 2;
          case 3 -> {
            // Modify a variable that is from a scope outside the expression.
            value++;
            yield 3;
          }
          default -> 0;
        };

    assertEquals(1, value);
  }

  private static void testSwitchNull() {
    assertThrowsNullPointerException(
        () -> {
          int result = getBoxedIntValue(null);
          // Use the result in a meaningful way to prevent the removal of the call to
          // getBoxedIntValue() because of being considered side effect free. The possibility of an
          // NPE due to a field dereference is not considered a side effect by jscompiler.
          fail("Returned " + result + " instead of throwing NPE");
        });
    assertThrowsNullPointerException(
        () -> {
          var result = getEnumValue(null);
          // Use the result in a meaningful way to prevent the removal of the call to getEnumValue()
          // because of being considered side effect free. The possibility of an NPE due to a field
          // dereference is not considered a side effect by jscompiler.
          fail("Returned " + result + " instead of throwing NPE");
        });
    assertThrowsNullPointerException(() -> getStringValue(null));
  }

  private static void testSwitchWithErasureCast() {
    Supplier<Numbers> supplier = () -> Numbers.ONE;
    int unused =
        // supplier.get() goes through the `T Supplier<T>.get()` method in the interface, and thus
        // must have an erasure cast. In this case it will trivially succeed.
        switch (supplier.get()) {
          case ONE -> 0;
          default -> {
            fail();
            yield 1;
          }
        };

    assertThrowsClassCastException(
        () -> {
          Supplier<Numbers> integerSupplier = (Supplier) () -> new Integer(1);
          int unusedResult =
              // supplier.get() goes through the `T Supplier<T>.get()` method in the interface, and
              // thus must have an erasure cast. In this case it should throw CCE and not just
              // fall into the default case.
              switch (integerSupplier.get()) {
                default -> {
                  fail();
                  yield 0;
                }
              };
        });
  }

  private static void testStringSwitch() {
    // Test that switch on strings uses .equals() semantics.
    assertTrue(getStringValue(new String(new char[] {'o', 'n', 'e'})) == 1);
  }

  private static void testThrow() {
    class ExceptionForSwitchExpression extends Exception {}

    int i = 0;
    try {
      int unused =
          switch (i) {
            default -> 1;
            case 0 -> throw new ExceptionForSwitchExpression();
          };
      fail();
    } catch (ExceptionForSwitchExpression expected) {
    }
  }

  private static void testAutoboxing() {
    // Having some yields boxed and some unboxed ensures that autoboxing in yield is occurring.
    assertTrue(0 == getYieldWithMixedBoxedAndUnboxedValue(0));
    assertTrue(1 == getYieldWithMixedBoxedAndUnboxedValue(1));

    // Make sure boxing happens even if both values are unboxed.
    Integer boxedValue =
        switch (0) {
          case 0 -> 2;
          default -> 1;
        };
    // Explicitly call Integer.intValue() to ensure that we catch the error if the boxing operation
    // was missing and boxedValue contained a primitive.
    assertTrue(2 == boxedValue.intValue());

    // Make sure boxing happens even if both values are boxed.
    int unboxedValue =
        switch (0) {
          case 0 -> (Integer) 1;
          default -> (Integer) 2;
        };

    if (TestUtils.isJavaScript()) {
      // Explicitly check that unboxedValue is actually a primitive integer to ensure that we catch
      // the error if the boxing operation was missing and boxedValue contained a primitive.
      assertIsPrimitiveInt(unboxedValue);
    }
    assertTrue(1 == unboxedValue);
  }

  private static int getYieldWithMixedBoxedAndUnboxedValue(int i) {
    Integer boxedZero = 0;
    return switch (i) {
      case 0 -> boxedZero;
      default -> 1;
    };
  }

  private static void assertIsPrimitiveInt(@DoNotAutobox Object object) {
    assertTrue(object instanceof Double);
  }
}
