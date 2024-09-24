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
package lambdas;

import javaemul.internal.annotations.Wasm;
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

  public void testNestedLambdas() {
    call(i -> call(j -> j, 20), 10);
  }

  public void testReturnLabelNameConflictKotlin() {
    call(
        i -> {
          FunctionalInterface:
          {
            return i;
          }
        },
        10);
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

  <T> GenericFunctionalInterface<T> callWithBiFunction(BiFunction<T, String, Double> fn) {
    return null;
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

  private static class Wrapper<T> {
    T wrapped = null;
  }

  public <T extends Enum<T>> void testLambdaWithGenerics() {
    callWithTypeVariable(i -> i, new Error());
    callParameterized(i -> i, new Error());
    callTypeVariableWithBounds(i -> i, (Enum<T>) null);
    callWithBiFunction(
        (x, y) -> {
          throw new RuntimeException();
        });
    callWithBiFunction((x, y) -> 3.0);
    Function<? super T, ?> f = item -> 1L;
    Function<Wrapper<String>, String> f2 = item -> item.wrapped;
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
    @Wasm("nop") // Taking a non-native argument in a native method not supported in Wasm.
    void then(GenericJsFunction<Void, T> f1, GenericJsFunction<Void, Throwable> f2);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  @java.lang.FunctionalInterface
  public interface AnotherThenable<T> {
    @Wasm("nop") // Taking a non-native argument in a native method not supported in Wasm.
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

  @Wasm("nop") // Converting lambdas to native types not supported in Wasm.
  public static void testJsInteropLambdas() {
    Thenable<String> thenable = (f1, f2) -> f1.apply(null);
    AnotherThenable<String> otherThenable = (f1, f2) -> f1.apply(null);
    JsSupplier<Integer> stringJsSupplier = () -> 1;
    stringJsSupplier.get();
    Equals equals = stringJsSupplier;
    equals.equals(null);
    equals.get();
  }

  interface JustADefaultT<T> {
    default void method(T t) {}
  }

  interface JustADefaultS<S> {
    default void method(S t) {}
  }

  // This is related to b/158085463.
  public static <U, V> void testIntersectionTyping() {
    // Shows the case where the types in the intersection declare type variables with the same name;
    // these type variables are actually different variables and should be unaliased.
    Object o = (GenericFunctionalInterface<String> & JustADefaultT<Number>) x -> x;
    // Shows the case where the types in the intersection declare type variables with the different
    // names; these type variables should be explicitly declared in the adaptor.
    o = (GenericFunctionalInterface<String> & JustADefaultS<Number>) x -> x;
    // Shows the case where type variables from the calling context are passed as type arguments
    // in the intersection types. If the non shared adaptor were to be specialized, these are
    // the variables that should be declared in it.
    o = (GenericFunctionalInterface<U> & JustADefaultS<V>) x -> x;
  }

  interface MarkerWithDefaultMethod {
    default void defaultMethod() {}
  }

  // This is related to b/158090696.
  public void testDefaultMethodsInIntersectionAdaptor() {
    Object o = (BiFunction<String, String, String> & MarkerWithDefaultMethod) (t, u) -> null;
  }

  // Iterable<T> interface is not functional in Kotlin, but in Java it is and should allow being
  // used as a lambda.
  public static <T> void testIterable(Iterable<T> iterable) {
    Iterable<T> lambda = () -> iterable.iterator();
  }

  interface Runnable {
    void run();
  }

  class Outer {
    void m() {}

    void n() {
      Runnable r = () -> this.m();
    }
  }

  class Super {
    void m() {}
  }

  class Sub extends Super {
    void n() {
      Runnable r =
          new Runnable() {
            public void run() {
              Sub.super.m();
            }
          };
    }
  }

  class SubWithLambda extends Super {
    void n() {
      Runnable r = () -> super.m();
    }
  }

  interface EmptyInterface {}

  interface EmptyInterfaceProvider {
    EmptyInterface provide();
  }

  static class ProviderHolder {
    public static final EmptyInterface emptyInterface = new EmptyInterface() {};
    public static final EmptyInterfaceProvider provideFromField = () -> emptyInterface;
    public static final EmptyInterfaceProvider provideFromAnonImpl = () -> new EmptyInterface() {};
  }
}

