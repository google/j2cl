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
package com.google.j2cl.transpiler.integration.lambdas;

import static com.google.j2cl.transpiler.utils.Asserts.assertEquals;
import static com.google.j2cl.transpiler.utils.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

@SuppressWarnings("MultipleTopLevelClasses")
interface MyInterface {
  int foo(int i);
}

public class Main {
  public static void main(String[] args) {
    Captures captures = new Captures();
    captures.testLambdaNoCapture();
    captures.testInstanceofLambda();
    captures.testLambdaCaptureField();
    captures.testLambdaCaptureLocal();
    captures.testLambdaCaptureFieldAndLocal();
    captures.testLambdaCaptureField2();
    testSpecialLambdas();
    testSpecializedLambda();
    testVarargsLambdas();
  }

  private static class Captures {
    private int field = 100;

    private int test(MyInterface intf, int n) {
      return this.field + intf.foo(n);
    }

    private int test(MyInterface intf) {
      this.field = 200;
      return this.field + intf.foo(300);
    }

    private void testLambdaNoCapture() {
      int result = test(i -> i + 1, 10);
      assertTrue(result == 111);
      result =
          test(
              i -> {
                return i + 2;
              },
              10);
      assertTrue(result == 112);
    }

    private void testInstanceofLambda() {
      MyInterface intf = (i -> i + 1);
      assertTrue((intf instanceof MyInterface));
    }

    private void testLambdaCaptureField() {
      int result = test(i -> field + i + 1, 10);
      assertTrue(result == 211);
    }

    private void testLambdaCaptureLocal() {
      int x = 1;
      int result = test(i -> x + i + 1, 10);
      assertTrue(result == 112);
    }

    private void testLambdaCaptureFieldAndLocal() {
      int x = 1;
      int result =
          test(
              i -> {
                int y = 1;
                return x + y + this.field + i + 1;
              },
              10);
      assertTrue(result == 213);
    }

    private void testLambdaCaptureField2() {
      int result = test(i -> field + i + 1);
      assertTrue((result == 701));
      assertTrue((this.field == 200));
    }
  }

  interface Equals<T> {
    @Override
    boolean equals(Object object);

    default T get() {
      return null;
    }
  }

  interface SubEquals extends Equals<String> {
    @Override
    String get();
  }

  @SuppressWarnings({"SelfEquals", "EqualsIncompatibleType"})
  private static void testSpecialLambdas() {
    SubEquals getHello = () -> "Hello";

    assertTrue(getHello.equals(getHello));
    assertTrue(!getHello.equals("Hello"));
    assertTrue("Hello".equals(getHello.get()));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void testSpecializedLambda() {
    Consumer<String> stringConsumer = s -> s.substring(1);
    Consumer rawConsumer = stringConsumer;
    assertThrowsClassCastException(() -> rawConsumer.accept(new Object()), String.class);

    VarargsIntFunction<String> firstA = ns -> ns[0].indexOf("a");
    VarargsIntFunction rawVarargsFunction = firstA;

    assertThrowsClassCastException(
        () -> rawVarargsFunction.apply(new Object[] {"bbabb", "aabb"}), String[].class);
  }

  interface Consumer<T> {
    void accept(T t);
  }

  interface VarargsIntFunction<T> {
    int apply(T... t);
  }

  private static void testVarargsLambdas() {
    VarargsFunction<String> changeFirstElement =
        ss -> {
          ss[0] = ss[0] + " world";
          return ss;
        };

    String[] params = new String[] {"hello"};
    assertEquals(params, changeFirstElement.apply(params));
    assertEquals("hello world", params[0]);
  }

  interface VarargsFunction<T> {
    T[] apply(T... t);
  }
}
