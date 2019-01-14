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
package com.google.j2cl.transpiler.integration.anonymousclasswithcaptures;

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

public class Main {
  interface AnonymousInterface {
    void foo();
  }

  abstract static class SomeClass {
    public int f;

    SomeClass(int f) {
      this.f = f;
    }

    public int foo() {
      return f;
    }
  }

  public void testAnonymousCaptures() {
    final Object[] instances = new Object[3];

    AnonymousInterface i =
        new AnonymousInterface() {
          Object cachedThis = this;

          @Override
          public void foo() {
            instances[0] = this;
            instances[1] = cachedThis;
            instances[2] = Main.this;
          }
        };

    i.foo();

    assertTrue(instances[0] == i);
    assertTrue(instances[1] == i);
    assertTrue(instances[2] == this);

    assertTrue(new SomeClass(3) {}.foo() == 3);
  }

  public static void main(String... args) {
    Main main = new Main();
    main.testAnonymousCaptures();
  }
}
