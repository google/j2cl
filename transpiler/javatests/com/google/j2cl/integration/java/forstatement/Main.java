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
package forstatement;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test for for loops.
 */
public class Main {
  public static void main(String... args) {
    int count = 0;

    for (int i = 0, j = 11; i < 10; i++) {
      j++;
      count = j + i;
    }

    assertTrue(count == 30);

    for (count = 0; count == 1; count++) {
      count = 100;
    }

    assertTrue(count == 0);

    for (int i = 0, j = 10; i < 10; i++, j--) {
      count = j - i;
    }
    assertTrue(count == -8);

    for (; count < 10; ) {
      count++;
    }
    assertTrue(count == 10);

    for (count = 0; count < 10; count++) ;

    assertTrue(count == 10);

    OUTER:
    for (int i = 0; i < 1; count++) {
      for (; ; count++) {
        i++;
        continue OUTER;
      }
    }

    assertTrue(count == 11);
  }
}
