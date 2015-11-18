package com.google.j2cl.transpiler.readable.jsproperties;

import jsinterop.annotations.JsProperty;

/**
 * Tests for non native static JsProperty.
 */
public class Foo {
  private static int f;

  @JsProperty
  public static int getA() {
    return f + 1;
  }

  @JsProperty
  public static void setA(int x) {
    f = x + 2;
  }

  @JsProperty(name = "abc")
  public static int getB() {
    return f + 3;
  }

  @JsProperty(name = "abc")
  public static void setB(int x) {
    f = x + 4;
  }
}
