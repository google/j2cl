package com.google.j2cl.transpiler.integration.jsfunction;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;

public class Main {
  @JsFunction
  interface Function {
    boolean invoke();

    @JsOverlay int f = 1;

    @JsOverlay
    default int fun() {
      return f + (invoke() ? 1 : 2);
    }
  }

  public static void main(String... args) {
    test();
  }

  public static void test() {
    assert ((Function) (() -> true)).fun() == 2;
    assert ((Function) (() -> false)).fun() == 3;
  }
}
