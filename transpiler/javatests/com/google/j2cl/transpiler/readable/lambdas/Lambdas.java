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
package com.google.j2cl.transpiler.readable.lambdas;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@SuppressWarnings("unused")
public class Lambdas {
  public int field = 100;

  interface FunctionalInterface {
    int m(int i);
  }

  public int call(FunctionalInterface intf, int n) {
    return this.field + intf.m(n);
  }

  public void testLambdaExpressionStyle() {
    call(i -> i + 1, 10);
  }

  public void testLambdaBlockStyle() {
    call(
        i -> {
          return i + 2;
        },
        10);
  }

  public void testLambdaCaptureField() {
    call(i -> field + i + 1, 10);
  }

  public void testLambdaCaptureLocal() {
    int x = 1;
    call(i -> x + i + 1, 10);
  }

  public void testLambdaCaptureFieldAndLocal() {
    int x = 1;
    call(
        i -> {
          int y = 1;
          return x + y + this.field + i + 1;
        },
        10);
  }

  public void funOuter() {}

  public void testLambdaCallOuterFunction() {
    call(
        i -> {
          funOuter();
          this.funOuter();
          Lambdas.this.funOuter();
          return i + 2;
        },
        10);
  }

  public static void testLambdaInStaticContext() {
    FunctionalInterface f = (i) -> i;
  }

  interface Functional<T> {
    Functional<T> wrap(Functional<T> f);
  }

  @SuppressWarnings("unused")
  public <T> void testInstanceMethodTypeVariableThroughLambda() {
    Functional<T> wrapped =
        (f) ->
            new Functional<T>() {
              @Override
              public Functional<T> wrap(Functional<T> f) {
                return null;
              }
            };
  }

  @SuppressWarnings("unused")
  public static <T> void testStaticMethodTypeVariableThroughLambda() {
    Functional<T> wrapped =
        (f) ->
            new Functional<T>() {
              @Override
              public Functional<T> wrap(Functional<T> f) {
                return null;
              }
            };
  }

  interface GenericFunctionalInterface<T> {
    T m(T i);
  }

  public <T> T callWithTypeVariable(GenericFunctionalInterface<T> intf, T e) {
    return intf.m(e);
  }

  public Error callParameterized(GenericFunctionalInterface<Error> intf, Error e) {
    return intf.m(e);
  }

  public static <T extends Enum<T>> Enum<T> callTypeVariableWithBounds(
      GenericFunctionalInterface<Enum<T>> intf, Enum<T> e) {
    return intf.m(e);
  }

  // Lambdas with default methods.
  interface BiFunction<T, U, R> {
    R apply(T t, U u);

    default <V> BiFunction<T, U, V> andThen(Function<? super R, ? extends V> after) {
      return (t, u) -> after.apply(this.apply(t, u));
    }
  }

  interface Function<T, R> {
    static <T> Function<T, T> identity() {
      return t -> t;
    }

    R apply(T t);
  }

  public <T extends Enum<T>> void testLambdaWithGenerics() {
    callWithTypeVariable(i -> i, new Error());
    callParameterized(i -> i, new Error());
    callTypeVariableWithBounds(i -> i, (Enum<T>) null);
    Function<? super T, ?> f = item -> 1L;
  }

  public static Object m() {
    return null;
  }

  public static void testLambdaCallingStaticMethod() {
    Function<Object, ?> f = l -> m();
  }

  interface FunctionalInterfaceWithMethodReturningVoid {
    void run();
  }

  public void testLambdaReturningVoidAsExpression() {
    FunctionalInterfaceWithMethodReturningVoid runner = () -> new Object();
  }

  public void testAnonymousInsideLambda() {
    FunctionalInterfaceWithMethodReturningVoid runner = () -> new Object() {};
  }

  private static class Parent {
    public int fieldInParent;

    public void funInParent() {}
  }

  private static class LambdaInSubClass extends Parent {
    public void testLambdaInSubclass() {
      FunctionalInterface l =
          (i -> {
            // call to outer class's inherited function
            funInParent(); // this.outer.funInParent()
            this.funInParent(); // this.outer.funInParent()
            LambdaInSubClass.this.funInParent(); // this.outer.funInParent()

            // access to outer class's inherited fields
            int a = fieldInParent;
            a = this.fieldInParent;
            a = LambdaInSubClass.this.fieldInParent;
            return a;
          });
    }
  }

  @JsFunction
  public interface GenericJsFunction<R, T> {
    R apply(T t);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  public interface Thenable<T> {
    void then(GenericJsFunction<Void, T> f1, GenericJsFunction<Void, Throwable> f2);
  }

  // @Nullable
  private static IdentityFunction identityFunction = null;

  /** Returns the identity function. */
  @SuppressWarnings("unchecked")
  public static <E> GenericJsFunction<E, E> identity() {
    if (identityFunction == null) {
      // Lazy initialize the field.
      identityFunction = new IdentityFunction();
    }
    return (GenericJsFunction<E, E>) identityFunction;
  }

  private static final class IdentityFunction implements GenericJsFunction<Object, Object> {
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

  public static void testJsInteropLambdas() {
    Thenable<String> rv = (f1, f2) -> f1.apply(null);
    JsSupplier<Integer> stringJsSupplier = () -> 1;
    stringJsSupplier.get();
    Equals equals = stringJsSupplier;
    equals.equals(null);
    equals.get();
  }
}
