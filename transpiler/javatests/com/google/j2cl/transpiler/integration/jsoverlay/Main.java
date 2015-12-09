package com.google.j2cl.transpiler.integration.jsoverlay;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

public class Main {
  @JsType(isNative = true, namespace = "test.foo")
  static class NativeJsTypeWithOverlay {
    public native int m();

    @JsOverlay
    public final int callM() {
      return m();
    }

    @JsOverlay
    public static final int fun(int a, int b) {
      return a > b ? a + b : a * b;
    }
  }

  public static void testNativeJsWithOverlay() {
    NativeJsTypeWithOverlay object = new NativeJsTypeWithOverlay();
    assert 6 == object.callM();
    assert 20 == NativeJsTypeWithOverlay.fun(4, 5);
  }

  public static void main(String... args) {
    testNativeJsWithOverlay();
  }
}
