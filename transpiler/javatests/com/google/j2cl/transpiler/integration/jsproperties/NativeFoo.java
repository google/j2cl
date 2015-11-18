package com.google.j2cl.transpiler.integration.jsproperties;

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
