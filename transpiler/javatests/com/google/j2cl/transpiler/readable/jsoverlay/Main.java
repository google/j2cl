package com.google.j2cl.transpiler.readable.jsoverlay;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

public class Main {
  @JsType(isNative = true, namespace = "test.foo")
  public static class NativeJsTypeWithOverlay {
    public native int m();

    @JsOverlay
    public final int callM() {
      return m();
    }

    @JsOverlay
    public static final int fun() {
      return 1;
    }
  }

  public void test() {
    NativeJsTypeWithOverlay n = new NativeJsTypeWithOverlay();
    n.callM();
    NativeJsTypeWithOverlay.fun();
  }
}
