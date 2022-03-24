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
package labeledstatement;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test for labeled statements.
 */
public class Main {
  public static void main(String... args) {
    int i = 0;
    int j = 0;

    LABEL:
    for (; i < 100; i++) {
      for (j = 0; j < 10; j++) {
        if (i == 10 && j == 5) {
          break LABEL;
        }
      }
    }

    assertTrue(i == 10);
    assertTrue(j == 5);

    i = 0;
    j = 0;
    int count = 0;

    // Use the same label as before. Labels need only to be unique from the nesting perspective.
    LABEL:
    for (; i < 100; i++) {
      for (j = 0; j < 10; j++) {
        if (j == 5) {
          continue LABEL;
        }
        count++;
      }
    }

    assertTrue(i == 100);
    assertTrue(j == 5);
    assertTrue(count == 500);

    count = 0;
    boolean unreachable = true;
    SKIP:
    NESTED_LABEL:
    {
      if (count == 0) {
        break SKIP; // jumps to the end of the labeled block.
      }
      unreachable = false;
    }
    assertTrue(unreachable);

    count = 0;
    BREAK:
    CONTINUE:
    for (; ; count++) {
      if (count > 1) {
        break BREAK;
      }

      continue CONTINUE;
    }
    assertTrue(count == 2);
  }
}
