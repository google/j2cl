package com.google.j2cl.transpiler.readable.jsproperties;

import jsinterop.annotations.JsProperty;

/**
 * Tests for native JsProperty.
 */
public class NativeFoo {
  @JsProperty(name = "hasOwnProperty")
  public native Object getA();

  @JsProperty(name = "PI", namespace = "Math")
  public static native double getB();
}
