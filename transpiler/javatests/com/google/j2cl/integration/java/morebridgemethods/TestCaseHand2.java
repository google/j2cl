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

import java.util.function.Consumer;

public class TestCaseHand2 {

  static interface I<I1> {
    String get(Consumer<? super I1> consumer);
  }

  abstract static class B<B1, B2> implements I<B1> {
    public String get(B2 consumer) {
      return "B get B2";
    }
  }

  static class C<C1> extends B<C1, Consumer<? super C1>> implements I<C1> {}

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void test() {
    C c = new C();

    assertTrue(((B) c).get("").equals("B get B2"));
    assertTrue(((B) c).get((String) null).equals("B get B2"));
    assertTrue(c.get((String) null).equals("B get B2"));
    assertTrue(((I) c).get(null).equals("B get B2"));
  }
}
