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
package morebridgemethods;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsType;

public class TestCase1596 {
  static interface BI1 {
    default String get(String value) {
      return "BI1 get String";
    }
  }

  @JsType
  static class B<B1> implements BI1 {
    public String get(B1 value) {
      return "B get B1";
    }
  }

  static class C extends B<String> {
    @JsConstructor
    public C() {}
  }

  public static void test() {
    C c = new C();
    assertTrue(((B) c).get("").equals("B get B1"));
    assertTrue(c.get("").equals("B get B1"));
    assertTrue(((BI1) c).get("").equals("B get B1"));
  }
}
