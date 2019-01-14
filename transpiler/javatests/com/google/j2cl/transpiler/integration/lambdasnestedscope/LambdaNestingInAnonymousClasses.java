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
 * Test lambdas nested in anonymous classes.
 */
public class LambdaNestingInAnonymousClasses {
  public void test() {
    int x = 42;

    // test lambda nested in anonymous class.
    int result =
        new MyInterface() {

          @Override
          public int fun(int a) {
            MyInterface intf = i -> i + x;
            return intf.fun(a) + 100;
          }
        }.fun(100);
    assertTrue(result == 242);

    // test lambda nested in multiple anonymous class.
    int[] y = new int[] {42};
    result =
        new MyInterface() {

          @Override
          public int fun(int a1) {
            return new MyInterface() {

              @Override
              public int fun(int a2) {
                return new MyInterface() {

                  @Override
                  public int fun(int a3) {
                    MyInterface intf = i -> y[0] = y[0] + i + a1 + a2 + a3;
                    return intf.fun(1000);
                  }
                }.fun(2000);
              }
            }.fun(3000);
          }
        }.fun(4000);
    assertTrue(result == 10042);
    assertTrue(y[0] == 10042);
  }
}
