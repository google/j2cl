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

  @JsType(isNative = true, namespace = JsPackage.GLOBAL)
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
