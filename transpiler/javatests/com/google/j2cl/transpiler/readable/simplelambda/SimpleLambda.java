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
package com.google.j2cl.transpiler.readable.simplelambda;

interface MyInterface {
  int foo(int i);
}

public class SimpleLambda {
  public int field = 100;

  public void funOuter() {}

  public int test(MyInterface intf, int n) {
    return this.field + intf.foo(n);
  }

  public void testLambdaNoCapture() {
    int result = test(i -> i + 1, 10);
    result =
        test(
            i -> {
              return i + 2;
            },
            10);
  }

  public void testLambdaCaptureField() {
    int result = test(i -> field + i + 1, 10);
  }

  public void testLambdaCaptureLocal() {
    int x = 1;
    int result = test(i -> x + i + 1, 10);
  }

  public void testLambdaCaptureFieldAndLocal() {
    int x = 1;
    int result =
        test(
            i -> {
              int y = 1;
              return x + y + this.field + i + 1;
            },
            10);
  }

  public void testLambdaCallOuterFunction() {
    int result =
        test(
            i -> {
              funOuter();
              this.funOuter();
              SimpleLambda.this.funOuter();
              return i + 2;
            },
            10);
  }

  public static void testLambdaInStaticContext() {
    MyInterface f = (i) -> i;
  }

  interface Functional<T> {
    Functional<T> wrap(Functional<T> f);
  }

  public <T> void testMethodTypeVariableThrowLambda() {
    Functional<T> wrapped =
        (f) ->
            new Functional<T>() {
              @Override
              public Functional<T> wrap(Functional<T> f) {
                return null;
              }
            };
  }
}
