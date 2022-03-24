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
package jsbridgemultipleaccidental;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  public static void main(String... args) {
    InterfaceOne a = new Test();
    InterfaceTwo b = new Test();
    InterfaceThree c = new Test();
    C d = new Test();
    Test e = new Test();

    assertTrue((a.fun(1) == 1));
    assertTrue((b.fun(1) == 1));
    assertTrue((c.fun(1) == 1));
    assertTrue((d.fun(1) == 1));
    assertTrue((e.fun(1) == 1));
  }
}
