/*
 * Copyright 2021 Google Inc.
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
package wideningfloatconversion;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import com.google.j2cl.integration.testing.TestUtils;

public class Main {
  public static void main(String... args) {
    testAssignment();
    testCast();
  }

  private static void testCast() {
    int mi = 2147483647; // Integer.MAX_VALUE;
    long ll = 2415919103L; // max_int < ll < max_int * 2, used for testing for signs.

    if (TestUtils.isJavaScript()) {
      // we don't honor float-double precision differences in JS
      assertTrue(((float) mi == 2.147483647E9));
      assertTrue(((float) ll == 2.415919103E9));
    } else {
      // JVM and Wasm
      assertTrue(((float) mi == 2.1474836E9f));
      assertTrue(((float) ll == 2.415919E9f));
    }
  }

  private static void testAssignment() {
    int mi = 2147483647; // Integer.MAX_VALUE;
    long ll = 2415919103L; // max_int < ll < max_int * 2, used for testing for signs.
    long ml = 9223372036854775807L; // Long.MAX_VALUE;

    float rf = 0;

    if (TestUtils.isJavaScript()) {
      // we don't honor float-double precision differences in JS
      assertTrue(((rf = mi) == 2.147483647E9));
      assertTrue(((rf = ll) == 2.415919103E9));
    } else {
      // JVM and Wasm
      assertTrue(((rf = mi) == 2.1474836E9f));
      assertTrue(((rf = ll) == 2.415919E9f));
      assertTrue(((rf = ml) == 9.223372E18f));
    }

    assertTrue(((rf = ml) == 9.223372036854776E18));
  }
}
