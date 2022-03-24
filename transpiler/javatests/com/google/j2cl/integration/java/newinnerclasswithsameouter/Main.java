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
package newinnerclasswithsameouter;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  public int f;

  public Main(int f) {
    this.f = f;
  }

  public class A {
    public void test() {
      // New instance of other inner classes with the same outer class.
      B b = new B();
      C c = new C();
      assertTrue((b.get() == 10));
      assertTrue((c.get() == 20));
    }
  }

  public class B {
    public int get() {
      return f;
    }
  }

  // private inner class
  private class C {
    public int get() {
      return f * 2;
    }
  }

  public static void main(String... args) {
    Main m = new Main(10);
    A a = m.new A();
    a.test();
  }
}
