package com.google.j2cl.transpiler.integration.buildtestexterns;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Foo", namespace = JsPackage.GLOBAL)
class FooOverlay {
  private String foo;

  /**
   * Returns the foo value.
   */
  @JsOverlay
  public final String getFoo() {
    return this.foo;
  }
}

/**
 * Integration test won't build without Main class with main function.
 */
public class Main {
  public static void main(String... args) {}
}
