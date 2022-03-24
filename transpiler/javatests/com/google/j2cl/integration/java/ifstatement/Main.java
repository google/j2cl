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
package ifstatement;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/** Test instanceof class type. */
public class Main {
  public static void main(String... args) {
    int count = 0;

    if (count == 0) {
      count = 1;
    } else {
      assertTrue(false);
    }

    assertTrue(count == 1);

    if (count == 0) {
      assertTrue(false);
    } else if (count == 1) {
      count = 2;
    } else {
      assertTrue(false);
    }

    if (count != 2) {
      assertTrue(false);
    } else if (count == 1) {
      assertTrue(false);
    } else {
      count = 3;
    }

    assertTrue(count == 3);

    // Make sure we add a block for ifs without a block
    if (count == 3) count = 4;
    assertTrue(count == 4);

    if (count != 4) assertTrue(false);
    else count = 5;

    assertTrue(count == 5);

    if (count == 5) ;
    else count = 6;

    assertTrue(count == 5);
  }
}
