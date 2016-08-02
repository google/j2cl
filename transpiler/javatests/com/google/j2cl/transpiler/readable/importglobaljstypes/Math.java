package com.google.j2cl.transpiler.readable.importglobaljstypes;

import static jsinterop.annotations.JsPackage.GLOBAL;

import jsinterop.annotations.JsType;

/**
 * Tests import global native js types.
 *
 * <p>In generated Math.impl.js, the NativeMath is imported as goog.require('global.Math')
 */
public class Math {
  public static double abs(double x) {
    return NativeMath.abs(x);
  }

  /**
   * Tests for type annotation for native types.
   */
  public NativeMath copy(NativeMath m) {
    return m;
  }

  @JsType(isNative = true, name = "Math", namespace = GLOBAL)
  public static class NativeMath {
    public static native double abs(double d);
  }
}
