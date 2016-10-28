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
