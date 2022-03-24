/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package continuestatement;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;

/**
 * Test for continue statement.
 */
public class Main {

  public static void main(String... args) {
    testForLoop();
    testWhileLoop();
    testNestedForLoop();
    testNestedSwitch();
  }

  private static void testForLoop() {
    int i = 0;

    for (; i < 100; i++) {
      if (i < 50) {
        continue;
      }
      break;
    }
    assertTrue(i == 50);
  }

  private static void testWhileLoop() {
    int i = 0;

    while (true) {
      i++;

      if (i < 100) {
        continue;
      }
      break;
    }

    assertTrue(i == 100);
  }

  private static void testNestedForLoop() {
    int count = 0;

    for (int j = 0; j < 100; j++) {
      for (int k = 0; k < 10; k++) {
        count++;
        if (count < 100) {
          continue;
        }
        break;
      }
      if (count < 100) {
        continue;
      }
      break;
    }

    assertTrue(count == 100);

    for (; ; ) {
      count++;
      if (count < 101) {
        continue;
      }
      break;
    }

    assertTrue(count == 101);
  }

  private static void testNestedSwitch() {
    boolean loop = true;

    while (loop) {
      switch (1) {
        case 1:
          switch (2) {
            case 2:
              assertTrue(loop);
              loop = false;
              continue;
          }
      }
      fail();
    }
  }
}
