package com.google.j2cl.transpiler.readable.importglobaljstypes;

import static jsinterop.annotations.JsPackage.GLOBAL;

import jsinterop.annotations.JsType;

/**
 * Tests import global native js types.
 *
 * <p>In generated Date.impl.js, the NativeDate is considered an extern and thus not goog.required
 */
public class Date {
  public static double now(double x) {
    return NativeDate.now();
  }

  /** Tests for type annotation for native types. */
  public NativeDate copy(NativeDate d) {
    return d;
  }

  @JsType(isNative = true, name = "Date", namespace = GLOBAL)
  public static class NativeDate {
    public static native double now();
  }
}
