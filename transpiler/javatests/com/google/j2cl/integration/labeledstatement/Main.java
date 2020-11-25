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
package com.google.j2cl.integration.labeledstatement;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test for labeled statements.
 */
public class Main {
  public static void main(String... args) {
    int i = 0;
    int j = 0;

    LABEL1:
    for (; i < 100; i++) {
      for (j = 0; j < 10; j++) {
        if (i == 10 && j == 5) {
          break LABEL1;
        }
      }
    }

    assertTrue(i == 10);
    assertTrue(j == 5);

    i = 0;
    j = 0;
    int count = 0;

    LABEL2:
    for (; i < 100; i++) {
      for (j = 0; j < 10; j++) {
        if (j == 5) {
          continue LABEL2;
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
    {
      if (count == 0) {
        break SKIP; // jumps to the end of the labeled block.
      }
      unreachable = false;
    }
    assertTrue(unreachable);
  }
}
