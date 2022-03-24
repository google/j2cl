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
package externalunqualifiedstaticfield;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test unqualified external static field reference.
 */
public class Main {
  public static void main(String[] args) {
    assertTrue(getEnumValue(Numbers.ONE) == 1);
    assertTrue(getEnumValue(Numbers.TWO) == 2);
    assertTrue(getEnumValue(Numbers.THREE) == 3);
  }

  private static int getEnumValue(Numbers numberValue) {
    switch (numberValue) {
      case ONE: // These unqualified static field references have a hidden implicit Numbers. prefix.
        return 1;
      case TWO: // These unqualified static field references have a hidden implicit Numbers. prefix.
        return 2;
      default:
        return 3;
    }
  }
}
