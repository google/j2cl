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
package localclassinstaticcontext;

public class LocalClassInStaticContext {
  public static Object f = new Object() {}; // Initializer in a static field declaration.

  public static void test() {
    // In a static function.
    class A {}
    new A();
    Object a =
        new Object() {
          void m() {
            // Here it is created in an instance context but the class is defined in a static one,
            // so no extra outer_this.
            new A();
          }
        };
  }

  static {
    // In a static block.
    class B {}
    new B();
    Object b = new Object() {};
  }

  public static class C {
    public int f = 1;

    public void test() {
      // It should have an enclosing instance, although its outer class is static.
      class D {
        public int fun() {
          return f;
        }
      }
      new D().fun();
    }
  }
}
