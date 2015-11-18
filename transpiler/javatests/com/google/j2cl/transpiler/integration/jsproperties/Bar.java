package com.google.j2cl.transpiler.integration.jsproperties;

import jsinterop.annotations.JsProperty;

/**
 * Tests for non native instance JsProperty.
 */
public class Bar {
  private int f = 10;

  public int getF() {
    return f;
  }

  @JsProperty
  public int getA() {
    return f + 1;
  }

  @JsProperty
  public void setA(int a) {
    f = a + 2;
  }

  @JsProperty(name = "abc")
  public int getB() {
    return f + 3;
  }

  @JsProperty(name = "abc")
  public void setB(int a) {
    f = a + 4;
  }
}
