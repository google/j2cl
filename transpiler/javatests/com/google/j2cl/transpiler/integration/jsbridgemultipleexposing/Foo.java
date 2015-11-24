package com.google.j2cl.transpiler.integration.jsbridgemultipleexposing;

import jsinterop.annotations.JsMethod;

class A {
  public int m() {
    return 1;
  }
}

class B extends A {
  @Override
  public int m() {
    return 5;
  }
}

interface I {
  int m();
}

public class Foo extends B implements I {
  @Override
  @JsMethod
  public int m() {
    return 10;
  }
}
