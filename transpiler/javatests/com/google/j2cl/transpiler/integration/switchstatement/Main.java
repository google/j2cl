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
package com.google.j2cl.transpiler.integration.switchstatement;

public class Main {
  public static void main(String... args) {
    assert getStringValue("zero") == 1; // Cascade
    assert getStringValue("one") == 1;
    assert getStringValue("two") == 2;
    assert getStringValue("three") == 3; // Default

    assert getCharValue('0') == 1; // Cascade
    assert getCharValue('1') == 1;
    assert getCharValue('2') == 2;
    assert getCharValue('3') == 3; // Default

    assert getIntValue(0) == 1; // Cascade
    assert getIntValue(1) == 1;
    assert getIntValue(2) == 2;
    assert getIntValue(3) == 3; // Default

    assert getEnumValue(Numbers.ZERO) == 1; // Cascade
    assert getEnumValue(Numbers.ONE) == 1;
    assert getEnumValue(Numbers.TWO) == 2;
    assert getEnumValue(Numbers.THREE) == 3; // Default

    testSwitchVariableDeclarations();
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
      case '2':
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

  private static int getEnumValue(Numbers numberValue) {
    switch (numberValue) {
      case ZERO:
      case ONE:
        return 1;
      case TWO:
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
        assert i == 3;
        return;
    }

    switch (5) {
      case 5:
        int i = 1;
        break;
    }
    assert false;
  }
}
