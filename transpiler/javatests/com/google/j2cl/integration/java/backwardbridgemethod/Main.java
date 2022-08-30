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
package backwardbridgemethod;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

interface I {
  String get(String s);
}

class C<T> {
  public T get(T t) {
    return t;
  }
}

/**
 * A bridge method for I.get(String):String should be generated, which delegates to C.get(T):T
 */
public class Main extends C<String> implements I {
  public static void main(String... args) {
    I i = new Main();
    C<String> c = new Main();
    Main m = new Main();
    String s = "";
    assertTrue(i.get(s) == s);
    assertTrue(c.get(s) == s);
    assertTrue(m.get(s) == s);
  }
}
