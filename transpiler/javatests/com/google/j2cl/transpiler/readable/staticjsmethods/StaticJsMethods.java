package com.google.j2cl.transpiler.readable.staticjsmethods;

import jsinterop.annotations.JsMethod;

public class StaticJsMethods {
  @JsMethod(name = "fun")
  public static void f1(int a) {}

  @JsMethod
  public static void f2(int a) {}

  @JsMethod(namespace = "Math", name = "floor")
  public static native void f3(double a);

  public void test() {
    StaticJsMethods.f1(1);
    f1(1);
    StaticJsMethods.f2(1);
    f2(1);
    StaticJsMethods.f3(1.1);
    f3(1.1);
  }
}
