package com.google.j2cl.transpiler.readable.transitivejsoverlayimport;

import jsinterop.annotations.JsConstructor;

public class NonNativeLower extends NonNativeUpper {
  @JsConstructor
  public NonNativeLower() {}
  void doNonNativeLowerInstanceMethod() {}
}
