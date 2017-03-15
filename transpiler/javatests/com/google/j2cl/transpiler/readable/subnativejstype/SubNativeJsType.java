package com.google.j2cl.transpiler.readable.subnativejstype;

import jsinterop.annotations.JsConstructor;

public class SubNativeJsType extends MyNativeJsType {
  @JsConstructor
  public SubNativeJsType(int x) {
    super(x + 1);
  }

  public SubNativeJsType() {
    this(10);
  }
}
