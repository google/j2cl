package com.google.j2cl.transpiler.integration.jsproperties;

import jsinterop.annotations.JsProperty;

/**
 * Tests for non native static JsProperty.
 */
public class Foo {
  private static int f = 10;

  public static int getF() {
    return f;
  }

  @JsProperty
  public static int getA() {
    return f + 1;
  }

  @JsProperty
  public static void setA(int a) {
    f = a + 2;
  }

  @JsProperty(name = "abc")
  public static int getB() {
    return f + 3;
  }

  @JsProperty(name = "abc")
  public static void setB(int a) {
    f = a + 4;
  }
}
