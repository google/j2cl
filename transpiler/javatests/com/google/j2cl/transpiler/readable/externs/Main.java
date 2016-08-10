package com.google.j2cl.transpiler.readable.externs;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class Main {
  static class FooImpl {
    @JsProperty String foo;
  }

  // Is overlaying extern "Foo", not Java class FooImpl. The extern Foo is just a @typedef that
  // declares a string field named "foo".
  @JsType(isNative = true, name = "Foo", namespace = JsPackage.GLOBAL)
  interface FooOverlay {
    /** Returns the foo value. */
    @JsProperty
    String getFoo();
  }

  public FooOverlay aFoo;

  private static boolean testFooOverlay(FooOverlay fooOverlay) {
    return fooOverlay.getFoo().equals("Hello");
  }

  private static native void useDirectlyAsFoo(Object fooOverlay);

  public static void main(String... args) {
    testFooOverlay((FooOverlay) (Object) new FooImpl());
    useDirectlyAsFoo(new FooImpl());
  }
}
