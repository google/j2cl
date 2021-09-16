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

enum Numbers {
  ONE,
  TWO
}

public class SwitchStatement {
  public void main() {
    // String switch.
    switch ("one") {
      case "one":
      case "two":
        break;
      default:
        return;
    }

    // Char switch.
    switch ('1') {
      case '1':
      case '2':
        break;
      default:
        return;
    }

    // Int switch.
    switch (1) {
      case -2:
      case 1:
      case 2:
        break;
      default:
        return;
    }

    // Enum switch.
    switch (Numbers.ONE) {
      case ONE:
      case TWO:
        break;
      default:
        return;
    }
  }

  @SuppressWarnings("unused")
  private static void testSwitchVariableDeclarations() {
    switch (3) {
      case 1:
        int unassigned, unassigned2;
        int i = 0;
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
