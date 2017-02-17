package com.google.j2cl.transpiler.integration.buildtestexterns;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class Main {
  static class FooImpl {
    @JsProperty public final String foo;

    public FooImpl(String value) {
      this.foo = value;
    }
  }

  // Is overlaying extern "Foo", not Java class FooImpl. The extern Foo is just a @typedef that
  // declares a string field named "foo".
  @JsType(isNative = true, name = "Foo", namespace = JsPackage.GLOBAL)
  interface FooOverlay {
    /** Returns the foo value. */
    @JsProperty
    String getFoo();
  }

  private static void testFooOverlay(FooOverlay fooOverlay) {
    assert fooOverlay.getFoo().equals("Hello");
  }

  public static void main(String... args) {
    testFooOverlay((FooOverlay) (Object) new FooImpl("Hello"));
  }
}
