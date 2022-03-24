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
package abstractinnerclass;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  interface A {
    int foo();
  }

  abstract static class B implements A {
    int bar() {
      return foo();
    }
  }

  abstract class C implements A {
    int bar() {
      return foo();
    }
  }

  static class BB extends B {
    @Override
    public int foo() {
      return 10;
    }
  }

  class CC extends C {
    @Override
    public int foo() {
      return 20;
    }
  }

  public static void main(String... args) {
    assertTrue(10 == new BB().bar());
    assertTrue(20 == new Main().new CC().bar());
  }
}
