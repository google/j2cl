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
package anonymousinnerclass;

class A {
  class B {
    
  }
}
public class AnonymousInnerClass {
  public class InnerClass {
  }
  public void test(final int arg) {
    InnerClass ic = new InnerClass() {}; // new an anonymous class with implicit qualifier.
    A a = new A();
    A.B b = a.new B(){}; // new an anonymous class with explicit qualifier.
    class C {
      public int fInC = arg;
    }
    C c = new C() {}; // new an anonymous local class.
  }
}
