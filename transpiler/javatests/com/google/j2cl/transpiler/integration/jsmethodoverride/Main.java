package com.google.j2cl.transpiler.integration.jsmethodoverride;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

public class Main {
  static class A {
    @JsMethod(name = "y")
    public int x() {
      return 1;
    }
  }

  @JsType
  static class B extends A {
    @Override
    public int x() {
      return 2;
    }
  }

  @JsMethod
  public static native int callY(Object o);

  public static void testJsMethodOverride() {
    A a = new A();
    B b = new B();
    assert 1 == callY(a);
    assert 2 == callY(b);
  }

  public static void main(String... args) {
    testJsMethodOverride();
  }
}
