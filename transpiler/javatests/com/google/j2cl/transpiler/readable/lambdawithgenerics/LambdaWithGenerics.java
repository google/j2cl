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
package com.google.j2cl.transpiler.readable.lambdawithgenerics;

import java.util.function.Function;

interface MyInterface<T> {
  T foo(T i);
}

public class LambdaWithGenerics<E> {
  public <T> T test1(MyInterface<T> intf, T e) {
    return intf.foo(e);
  }

  public Error test2(MyInterface<Error> intf, Error e) {
    return intf.foo(e);
  }

  public static <T extends Enum<T>> Enum<T> test3(MyInterface<Enum<T>> intf, Enum<T> e) {
    return intf.foo(e);
  }

  public static Object m() {
    return null;
  }

  public <T extends Enum<T>> void testLambdaNoCapture() {
    test1(i -> i, new Error());
    test2(i -> i, new Error());
    test3(i -> i, (Enum<T>) null);
    Function<? super T, ?> f = item -> 1L;
  }

  public static void testLabdaCallingStaticMethod() {
    Function<Object, ?> f = l -> m();
  }
}
