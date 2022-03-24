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
package ternaryexpression;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test ternary expression.
 */
public class Main {

  public static void main(String... args) {
    int count = true ? 1 : 2;
    assertTrue(count == 1);

    count = count == 2 ? 2 : 3;
    assertTrue(count == 3);

    int foo = ((2 * count) / 2) == 3 ? (4 + 4) / 2 : 5 + 5;
    assertTrue(foo == 4);

    foo = foo < 5 && foo > 3 ? (foo == 4 ? 5 : 6) : (0);
    assertTrue(foo == 5);

    foo = foo == 5 ? new Integer(15) : new Integer(30);
    assertTrue(foo == 15);
  }
}
