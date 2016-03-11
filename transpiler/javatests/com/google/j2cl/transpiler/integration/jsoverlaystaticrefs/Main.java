package com.google.j2cl.transpiler.integration.jsoverlaystaticrefs;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

public class Main {

  @JsType(isNative = true, namespace = "test.foo")
  static class NativeTypeWithStaticOverlay {
    @JsOverlay public static Object staticField = new Object();

    @JsOverlay
    private static final Object getStaticField() {
      return staticField;
    }
  }

  @JsType(isNative = true, namespace = "test.foo")
  static class NativeTypeWithInstanceOverlay {
    @JsOverlay public static Object staticField = new Object();

    @JsOverlay
    final Object getStaticField() {
      return staticField;
    }
  }

  public static void testNativeJsWithOverlay() {
    assert NativeTypeWithStaticOverlay.getStaticField() != null;

    assert new NativeTypeWithInstanceOverlay().getStaticField() != null;
  }

  public static void main(String... args) {
    testNativeJsWithOverlay();
  }
}
