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
package com.google.j2cl.transpiler.integration.lambdasnestedscope;

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

/**
 * Test lambda having access to local variables and arguments when placed in mixed scopes.
 * Local class -> local class -> anonymous -> lambda -> anonymous -> lambda
 */
public class MixedNesting {
  class A {
    public void a() {
      int[] x = new int[] {42};
      class B {
        public int b() {
          MyInterface i =
              new MyInterface() {

                @Override
                public int fun(int a) {
                  MyInterface ii =
                      n -> {
                        return new MyInterface() {
                          @Override
                          public int fun(int b) {
                            MyInterface iii = m -> x[0] = x[0] + a + b + n + m;
                            return iii.fun(100);
                          }
                        }.fun(200);
                      };
                  return ii.fun(300);
                }
              };
          return i.fun(400);
        }
      }
      int result = new B().b();
      assertTrue(result == 1042);
      assertTrue(x[0] == 1042);
    }
  }

  public void test() {
    new A().a();
  }
}
