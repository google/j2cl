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
package mixednestings;

interface MyInterface {
  int fun(int a);
}

/**
 * Test lambda having access to local variables and arguments when placed in mixed scopes.
 * Local class -> local class -> anonymous -> lambda -> lambda -> anonymous
 */
public class MixedNestings {
  public void mm() {}

  class A {
    public void aa() {}

    public void a() {
      class B {
        public void bb() {}

        public int b() {
          MyInterface i =
              new MyInterface() {

                @Override
                public int fun(int a) {
                  MyInterface ii =
                      n -> {
                        mm();
                        aa();
                        bb();
                        MyInterface iii =
                            m -> {
                              mm();
                              aa();
                              bb();
                              return new MyInterface() {
                                @Override
                                public int fun(int b) {
                                  return b;
                                }
                              }.fun(100);
                            };
                        return iii.fun(200);
                      };
                  return ii.fun(300);
                }
              };
          return i.fun(400);
        }
      }
      new B().b();
    }
  }

  public void test() {
    new A().a();
  }
}
