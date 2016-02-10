package com.google.j2cl.transpiler.readable.staticconstant;

public class StaticConstant {
  // This constant should not trigger a clinit rewrite.
  public static final int FOO = 1;
}
