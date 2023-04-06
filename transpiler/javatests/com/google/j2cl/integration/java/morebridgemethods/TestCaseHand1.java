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

import jsinterop.annotations.JsMethod;

public class TestCaseHand1 {
  private static class A<A1> {
    public String get(A1 a1) {
      return "A get Object";
    }
  }

  private static class B<B1> extends A<B1> {
    @JsMethod
    @Override
    public String get(B1 b1) {
      return "B get Object";
    }
  }

  private static class C extends B<String> {
    @Override
    public String get(String string) {
      return "C get String";
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void test() {
    C c = new C();
    assertTrue(((A) c).get("").equals("C get String"));
    assertTrue(((B) c).get("").equals("C get String"));
    assertTrue(c.get("").equals("C get String"));
  }
}
