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
package com.google.j2cl.transpiler.readable.lambdajsinterop;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {
  @JsFunction
  public interface JsFunc<R, T> {
    R execute(T t);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  public interface Thenable<T> {
    void then(JsFunc<Void, T> f1, JsFunc<Void, Throwable> f2);
  }

  @JsFunction
  interface Function<F, T> {

    /** Returns the result of applying this function to {@code input}. */
    T apply(F input);
  }
  // @Nullable
  private static IdentityFunction identityFunction = null;

  /** Returns the identity function. */
  @SuppressWarnings("unchecked")
  public static <E> Function<E, E> identity() {
    if (identityFunction == null) {
      // Lazy initialize the field.
      identityFunction = new IdentityFunction();
    }
    return (Function<E, E>) identityFunction;
  }

  private static final class IdentityFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object o) {
      return o;
    }
  }

  interface Equals<T> {
    boolean equals(Object o);

    default T get() {
      return null;
    }
  }

  interface JsSupplier<T extends Number> extends Equals<T> {
    @JsMethod
    T get();
  }

  public static void main() {
    Thenable<String> rv = (f1, f2) -> f1.execute(null);
    JsSupplier<Integer> stringJsSupplier = () -> 1;
    stringJsSupplier.get();
    Equals equals = stringJsSupplier;
    equals.equals(null);
    equals.get();
  }
}
