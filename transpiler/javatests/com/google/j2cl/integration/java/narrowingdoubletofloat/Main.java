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
package narrowingdoubletofloat;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  public static void main(String... args) {
    testCoercions();
  }

  private static void testCoercions() {
    double dd = 2415919103.7; // dd < Double.MAX_VALUE;
    double md = 1.7976931348623157E308; // Double.MAX_VALUE;

    assertTrue(((float) dd == dd)); // we don't honor float-double precision differences
    assertTrue(((float) md == md)); // we don't honor float-double precision differences
  }
}
