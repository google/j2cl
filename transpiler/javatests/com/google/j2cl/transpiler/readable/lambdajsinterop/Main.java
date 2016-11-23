package com.google.j2cl.transpiler.readable.lambdajsinterop;

import jsinterop.annotations.JsFunction;
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

  public static void main() {
    Thenable<String> rv = (f1, f2) -> f1.execute(null);
  }
}

class Functions {

  @JsFunction
  interface Function<F, T> {

    /** Returns the result of applying this function to {@code input}. */
    T apply(F input);
  }
  // @Nullable
  private static IdentityFunction identityFunction = null;

  private Functions() {}

  /** Returns the identity function. */
  @SuppressWarnings("unchecked")
  public static <E> Function<E, E> identity() {
    if (identityFunction == null) {
      // Lazy initialize the field.
      identityFunction = new IdentityFunction();
    }
    return (Function<E, E>) Functions.identityFunction;
  }

  private static class IdentityFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object o) {
      return o;
    }
  }
}
