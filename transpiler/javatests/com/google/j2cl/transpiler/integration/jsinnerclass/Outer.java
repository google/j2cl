package com.google.j2cl.transpiler.integration.jsinnerclass;

import jsinterop.annotations.JsType;

public class Outer {
  int a = 2;

  @JsType(namespace = "com.google.test", name = "Inner")
  public class Inner {
    private int b;

    public Inner() {
      this.b = a + 1;
    }

    public int getB() {
      return b;
    }
  }
}
