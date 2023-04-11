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
package forstatement;

public class ForStatement {
  public void test() {
    for (int i = 0, count = 0; i < 100; i++) {
      count++;
    }

    for (long l = 0, count = 0; l < 100; l++, l++) {
      count++;
    }

    boolean a = false;
    for (; a |= true; ) {}

    boolean b = false;
    for (; b = a; ) {}

    for (returnsValue(); ; returnsValue()) {}
  }

  public void testContinue() {
    LABEL:
    for (int i = 0, j = 0; i < 10; i++, j++) {
      if (i == 5) {
        continue;
      }
      for (int k = 0, l = 0; k < 10; k++, l++) {
        if (j == 5) {
          continue LABEL;
        }
      }
    }
  }

  public void testInitializeInForCondition() {
    for (int i = 0, x; i < 1 && (x = 3) != 0; i++) {
      int y;
      y = i + x;
    }
  }

  public void testForStatementInsideDeadCode() {
    if (false) {
      // The variable i is inferred to be effectively final by JDT because the code here is
      // unreachable.
      for (int i = 0; i < 100; i++) {}
    }
  }

  private static int returnsValue() {
    return 1;
  }
}
