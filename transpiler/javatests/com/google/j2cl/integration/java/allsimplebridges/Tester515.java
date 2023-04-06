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

import jsinterop.annotations.JsType;

public class Tester515 {
  @JsType
  static interface I1 {
    String get(String value);
  }

  static class C1 implements I1 {
    C1() {}

    @Override
    public String get(String value) {
      return "C1.get";
    }
  }

  public static void test() {
    C1 s = new C1();
    assertTrue(s.get("").equals("C1.get"));
    assertTrue(((I1) s).get("").equals("C1.get"));
  }
}
