package com.google.j2cl.transpiler.integration.instancejsmethods;

public class SuperParent {
  public int fun(int a, int b) {
    return a + b + 111;
  }

  public int bar(int a, int b) {
    return a * b + 222;
  }
}
