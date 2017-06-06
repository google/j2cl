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
package com.google.j2cl.transpiler.integration.allsimplebridges;

public class Tester897 {
  @SuppressWarnings("unchecked")
  static class C1 {
    C1() {}
    @SuppressWarnings("MissingOverride")
    public String get(Object value) {
      return "C1.get";
    }

    @SuppressWarnings("MissingOverride")
    public String get(String value) {
      return "C1.get";
    }
  }

  @SuppressWarnings("unchecked")
  public static void test() {
    C1 s = new C1();
    assert s.get(new Object()).equals("C1.get");
    assert s.get("").equals("C1.get");
  }
}
