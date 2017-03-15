package com.google.j2cl.transpiler.integration.subnativejstype;

import jsinterop.annotations.JsConstructor;

public class SubMyNativeType extends MyNativeType {
  public int f = 20;

  @JsConstructor
  public SubMyNativeType(int x, int y) {
    super(x + y, x * y);
    f += x - y;
  }
}
