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

import jsinterop.annotations.JsEnum;

public class SwitchPatterns {
  void fail() {}

  enum Color {
    RED,
    BLUE,
    GREEN
  }

  @JsEnum
  enum Digit {
    ZERO,
    ONE,
    TWO,
    THREE
  }

  void testNullPatterns() {
    Color color = null;
    // Enum switch. Case null that is not a default case.
    switch (color) {
      case RED:
        fail();
        break;
      case null:
        break;
      default:
        fail();
    }

    // Enum switch. Case null, default case.
    switch (color) {
      case RED:
        fail();
        break;
      case null, default:
        {
          int variable = 0;
          break;
        }
    }

    // Enum switch. Expression with potential side effects.
    switch (Color.valueOf("BLUE")) {
      case RED:
        fail();
        break;
      case null, default:
        break;
    }

    // JsEnum switch. Case null that is not a default case.
    Digit digit = null;
    switch (digit) {
      case ZERO:
        fail();
        break;
      case null:
        break;
      default:
        fail();
    }

    // String switch. Case null that is not a default case.
    String s = "Hi";
    switch (s) {
      case "Hi":
        fail();
        break;
      case null:
        break;
      default:
        fail();
    }

    // String switch. Case null, default case.
    switch (s) {
      case "Hi":
        fail();
        break;
      case null, default:
        break;
    }
  }
}
