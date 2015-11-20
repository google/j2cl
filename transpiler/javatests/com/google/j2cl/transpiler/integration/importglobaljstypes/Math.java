package com.google.j2cl.transpiler.integration.importglobaljstypes;

import jsinterop.annotations.JsType;

/**
 * Tests import global native js types.
 *
 * <p>In generated Math.impl.js, the NativeMath is imported as goog.require('global.Math')
 */
public class Math {
  public static int fun(int x) {
    return NativeMath.abs(x);
  }

  @JsType(isNative = true, name = "Math", namespace = "")
  public static class NativeMath {
    public static native int abs(int d);
  }
}
