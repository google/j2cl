/*
 * Copyright 2025 Google Inc.
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
package switchpatterns;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;
import static com.google.j2cl.integration.testing.TestUtils.getUndefined;

import jsinterop.annotations.JsEnum;

// TODO(b/465572498): Move the tests to the corresponding integration once javac is enabled for all
// backends.
public class Main {
  public static void main(String... args) {
    testSwitchPatterns();
    testSwitchValues();
    testSwitchSideEffectInExpression();
    testSwitchCaseEvaluationOrder();
    // TODO(b/465574821):Add tests for jsenums.
  }

  private static void testSwitchPatterns() {
    assertTrue(getSwitchPattern("Hello") == 1);
    assertTrue(getSwitchPattern("Bye") == 2);
    assertTrue(getSwitchPattern(44) == 3);
    assertTrue(getSwitchPattern(45) == 4); // default
    assertTrue(getSwitchPattern(true) == 4); // default
    assertThrowsNullPointerException(() -> getSwitchPattern(null));
    assertThrowsNullPointerException(() -> getSwitchPattern(getUndefined()));

    assertTrue(getSwitchPatternWithNull("Hello") == 1);
    assertTrue(getSwitchPatternWithNull("Bye") == 2);
    assertTrue(getSwitchPatternWithNull(44) == 3);
    assertTrue(getSwitchPatternWithNull(45) == 4); // default
    assertTrue(getSwitchPatternWithNull(true) == 4); // default
    assertTrue(getSwitchPatternWithNull(null) == 5);
    assertTrue(getSwitchPatternWithNull(getUndefined()) == 5);

    assertTrue(getSwitchPatternWithExpressions(42) == 1);
    assertTrue(getSwitchPatternWithExpressions(43) == 1);
    assertTrue(getSwitchPatternWithExpressions(4) == 2);
    assertTrue(getSwitchPatternWithExpressions(5) == 4);
    assertThrowsNullPointerException(() -> getSwitchPatternWithExpressions(null));
    assertThrowsNullPointerException(() -> getSwitchPatternWithExpressions(getUndefined()));

    assertTrue(getSwitchPatternWithExpressionsAndNull(42) == 1);
    assertTrue(getSwitchPatternWithExpressionsAndNull(43) == 1);
    assertTrue(getSwitchPatternWithExpressionsAndNull(4) == 2);
    assertTrue(getSwitchPatternWithExpressionsAndNull(5) == 4);
    assertTrue(getSwitchPatternWithExpressionsAndNull(null) == 3);
    assertTrue(getSwitchPatternWithExpressionsAndNull(getUndefined()) == 3);

    assertTrue(getSwitchPatternWithRecord(new R(new R("Hello", "Bye"), null)) == 3);
    assertTrue(getSwitchPatternWithRecord(new R("Hello", "Bye")) == 2);
    assertTrue(getSwitchPatternWithRecord("Hello") == 42);
    assertThrowsNullPointerException(() -> getSwitchPatternWithRecord(null));
    assertThrowsNullPointerException(() -> getSwitchPatternWithRecord(getUndefined()));
  }

  private static int getSwitchPattern(Object o) {
    return switch (o) {
      case String s when s.length() == 5 -> 1;
      case Integer i when i % 2 == 0 -> 3;
      case String s -> 2;
      default -> 4;
    };
  }

  private static int getSwitchPatternWithNull(Object o) {
    return switch (o) {
      case String s when s.length() == 5 -> 1;
      case Integer i when i % 2 == 0 -> 3;
      case null -> 5;
      case String s -> 2;
      default -> 4;
    };
  }

  private static int getSwitchPatternWithExpressions(Integer i) {
    return switch (i) {
      case 42, 43 -> 1;
      case Integer j when j % 2 == 0 -> 2;
      case Number n -> 4;
    };
  }

  private static int getSwitchPatternWithExpressionsAndNull(Integer i) {
    return switch (i) {
      // Note that the comparison to 42 must not NPE if i is null.
      case 42, 43 -> 1;
      case Integer j when j % 2 == 0 -> 2;
      case null -> 3;
      case Number n -> 4;
    };
  }

  private static record R(Object o, String s) {}

  private static int getSwitchPatternWithRecord(Object o) {
    return switch (o) {
      case R(R(String s1, var s2), var s3) when s1.equals("Hello") -> s2.length();
      case R(var c1, var c2) -> 2;
      default -> 42;
    };
  }

  private static void testSwitchValues() {
    assertTrue(getStringValueWithNull("zero") == 1);
    assertTrue(getStringValueWithNull("one") == 1);
    assertTrue(getStringValueWithNull("two") == 2);
    assertTrue(getStringValueWithNull("three") == 4); // Default
    assertTrue(getStringValueWithNull(null) == 3);
    assertTrue(getStringValueWithNull(getUndefined()) == 3);

    assertTrue(getStringValueWithNullDefault("zero") == 1);
    assertTrue(getStringValueWithNullDefault("one") == 1);
    assertTrue(getStringValueWithNullDefault("two") == 2);
    assertTrue(getStringValueWithNullDefault("three") == 3); // Default
    assertTrue(getStringValueWithNullDefault(null) == 3);
    assertTrue(getStringValueWithNullDefault(getUndefined()) == 3);

    assertTrue(getEnumValueWithNull(Numbers.ZERO) == 1);
    assertTrue(getEnumValueWithNull(Numbers.ONE) == 1);
    assertTrue(getEnumValueWithNull(Numbers.TWO) == 2);
    assertTrue(getEnumValueWithNull(Numbers.THREE) == 4); // Default
    assertTrue(getEnumValueWithNull(null) == 3);
    assertTrue(getEnumValueWithNull(getUndefined()) == 3);

    assertTrue(getEnumValueWithNullDefault(Numbers.ZERO) == 1);
    assertTrue(getEnumValueWithNullDefault(Numbers.ONE) == 1);
    assertTrue(getEnumValueWithNullDefault(Numbers.TWO) == 2);
    assertTrue(getEnumValueWithNullDefault(Numbers.THREE) == 3); // Default
    assertTrue(getEnumValueWithNullDefault(null) == 3); // Default
    assertTrue(getEnumValueWithNullDefault(getUndefined()) == 3); // Default

    assertTrue(getJsEnumValueWithNull(JsNumbers.ZERO) == 1);
    assertTrue(getJsEnumValueWithNull(JsNumbers.ONE) == 1);
    assertTrue(getJsEnumValueWithNull(JsNumbers.TWO) == 2);
    assertTrue(getJsEnumValueWithNull(JsNumbers.THREE) == 4); // Default
    assertTrue(getJsEnumValueWithNull(null) == 3);
    assertTrue(getJsEnumValueWithNull(getUndefined()) == 3);

    assertTrue(getJsEnumValueWithNullDefault(JsNumbers.ZERO) == 1);
    assertTrue(getJsEnumValueWithNullDefault(JsNumbers.ONE) == 1);
    assertTrue(getJsEnumValueWithNullDefault(JsNumbers.TWO) == 2);
    assertTrue(getJsEnumValueWithNullDefault(JsNumbers.THREE) == 3); // Default
    assertTrue(getJsEnumValueWithNullDefault(null) == 3); // Default
    assertTrue(getJsEnumValueWithNullDefault(getUndefined()) == 3); // Default
  }

  private static int getStringValueWithNull(String stringValue) {
    return switch (stringValue) {
      case null -> 3;
      case "zero", "one" -> 1;
      default -> 4;
      case "two" -> 2;
    };
  }

  private static int getStringValueWithNullDefault(String stringValue) {
    return switch (stringValue) {
      case "zero", "one" -> 1;
      case "two" -> 2;
      case null, default -> 3;
    };
  }

  public enum Numbers {
    ZERO,
    ONE,
    TWO,
    THREE
  }

  private static int getEnumValueWithNull(Numbers numberValue) {
    return switch (numberValue) {
      case ZERO, ONE -> 1;
      case null -> 3;
      default -> 4;
      case TWO -> {
        yield 2;
      }
    };
  }

  private static int getEnumValueWithNullDefault(Numbers numberValue) {
    return switch (numberValue) {
      case ZERO, ONE -> 1;
      case TWO -> {
        yield 2;
      }
      case null, default -> 3;
    };
  }

  private static void testSwitchSideEffectInExpression() {
    sideeffects = 0;
    switch (getEnumValueWithSideEffect()) {
      case ZERO, ONE:
        break;
      case null, default:
        fail();
    }

    assertEquals(1, sideeffects);
  }

  private static int sideeffects = 0;

  private static Numbers getEnumValueWithSideEffect() {
    sideeffects++;
    return Numbers.ONE;
  }

  @JsEnum
  enum JsNumbers {
    ZERO,
    ONE,
    TWO,
    THREE
  }

  private static int getJsEnumValueWithNull(JsNumbers numberValue) {
    return switch (numberValue) {
      case ZERO, ONE -> 1;
      case null -> 3;
      default -> 4;
      case TWO -> {
        yield 2;
      }
    };
  }

  private static int getJsEnumValueWithNullDefault(JsNumbers numberValue) {
    return switch (numberValue) {
      case ZERO, ONE -> 1;
      case TWO -> {
        yield 2;
      }
      case null, default -> 3;
    };
  }

  static class SideEffectTracker {
    String sideEffects = "";

    boolean test(String testString, boolean succeed) {
      sideEffects += testString;
      return succeed;
    }
  }

  private static void testSwitchCaseEvaluationOrder() {
    SideEffectTracker s = new SideEffectTracker();
    switch (s) {
      case SideEffectTracker st when st.test("1", false) -> {}
      case SideEffectTracker st when st.test("2", true) -> {}
      default -> {}
    }

    assertEquals("12", s.sideEffects);
  }
}
