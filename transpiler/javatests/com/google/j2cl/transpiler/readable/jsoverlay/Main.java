package com.google.j2cl.transpiler.readable.jsoverlay;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

public class Main {
  @JsType(isNative = true, namespace = "test.foo")
  public static class NativeJsTypeWithOverlay {
    public static int nonJsOverlayField;

    @JsOverlay public static final int COMPILE_TIME_CONSTANT = 10;

    @JsOverlay public static Object staticField = new Object();

    public native int m();

    // non JsOverlay static method.
    public static native void n();

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
    NativeJsTypeWithOverlay.n();
    int a =
        NativeJsTypeWithOverlay.COMPILE_TIME_CONSTANT + NativeJsTypeWithOverlay.nonJsOverlayField;
    NativeJsTypeWithOverlay.staticField = null;
  }
}
