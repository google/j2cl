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
package com.google.j2cl.transpiler.integration.lambdaswithgenerics;

import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.Supplier;

interface MyInterface<T> {
  T foo(T i);
}

public class Main {
  public int field;

  public Main(int f) {
    this.field = f;
  }

  public int test(MyInterface<Main> intf, Main m) {
    return this.field + intf.foo(m).field;
  }

  public void testLambdaNoCapture() {
    Main m = new Main(100);
    int result = test(i -> new Main(i.field * 3), m);
    assert (result == 310);
  }

  public void testLambdaCaptureField() {
    Main m = new Main(100);
    int result =
        test(
            i -> {
              int f = i.field + field;
              return new Main(f);
            },
            m);
    assert (result == 120);
  }

  public void testLambdaCaptureLocal() {
    final Main m = new Main(100);
    int result =
        test(
            i -> {
              int f = i.field + m.field;
              return new Main(f);
            },
            m);
    assert (result == 210);
  }

  public static void main(String[] args) {
    Main m = new Main(10);
    m.testLambdaNoCapture();
    m.testLambdaCaptureField();
    m.testLambdaCaptureLocal();
  }

  // The next definitions are testing that types variables are created correctly in the lambda
  // class implementations and if not the compile through jscompiler will fail.
  public interface Stream<T> {
    <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

    static <T> Stream<T> generate(Supplier<T> s) {
      return null;
    }
  }

  public static <T> Stream<T> stream(Supplier<? extends Spliterator<T>> supplier) {
    return Stream.generate(supplier).flatMap(spliterator -> stream(spliterator));
  }

  public static <T> Stream<T> stream(Spliterator<T> spliterator) {
    return null;
  }
}
