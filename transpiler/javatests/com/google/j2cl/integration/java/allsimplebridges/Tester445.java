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
package allsimplebridges;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Tester445 {
  static class C1 {
    C1() {}
    public String get(Object value) {
      return "C1.get";
    }
  }

  static class C2 extends C1 {
    C2() {}

    @Override
    public String get(Object value) {
      return "C2.get";
    }
  }

  public static void test() {
    C2 s = new C2();
    assertTrue(s.get(new Object()).equals("C2.get"));
    assertTrue(((C1) s).get("").equals("C2.get"));
  }
}
