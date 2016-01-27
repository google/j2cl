package com.google.j2cl.transpiler.readable.subnativejstype;

public class SubNativeJsType extends MyNativeJsType {
  public SubNativeJsType(int x) {
    super(x + 1);
  }

  public SubNativeJsType() {
    this(10);
  }
}
