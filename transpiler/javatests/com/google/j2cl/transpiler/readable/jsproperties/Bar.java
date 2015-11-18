package com.google.j2cl.transpiler.readable.jsproperties;

import jsinterop.annotations.JsProperty;

/**
 * Tests for non native instance JsProperty.
 */
public class Bar {
  private int f;

  @JsProperty
  public int getA() {
    return f + 1;
  }

  @JsProperty
  public void setA(int x) {
    f = x + 2;
  }

  @JsProperty(name = "abc")
  public int getB() {
    return f + 3;
  }

  @JsProperty(name = "abc")
  public void setB(int x) {
    f = x + 4;
  }
}
