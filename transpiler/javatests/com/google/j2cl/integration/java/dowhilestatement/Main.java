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
package dowhilestatement;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test do while loop.
 */
public class Main {
  public static void main(String... args) {
    int count = 0;

    do {
      count++;
    } while (count < 5);

    do count++;
    while (count < 10);
    assertTrue(count == 10);

    do ;
    while (count++ < 14);
    assertTrue(count == 15);
  }
}
