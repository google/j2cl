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
package simpleliterals;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  @SuppressWarnings("cast")
  public static void main(String... args) {
    boolean a = false;
    assertTrue(!a);

    char b = 'a';
    assertTrue(b == 97);

    // Verify compile time float literal expansion to double.
    assertTrue(0.7308782f == (float) 0.7308781743049622d);

    // Avoiding a "condition always evaluates to true" error in JSComp type checking.
    Object maybeNull = b == 97 ? null : new Object();
    Object c = null;
    assertTrue(c == maybeNull);

    int d = 100;
    assertTrue(d == 50 + 50);

    String e = "foo";
    assertTrue(e instanceof String);

    Class<?> f = Main.class;
    assertTrue(f instanceof Class);
  }
}
