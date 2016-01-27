package com.google.j2cl.transpiler.integration.subnativejstype;

public class SubMyNativeType extends MyNativeType {
  public int f = 20;

  public SubMyNativeType(int x, int y) {
    super(x + y, x * y);
    f += x - y;
  }
}
