package com.google.j2cl.transpiler.integration.instancejsmethods;

import jsinterop.annotations.JsMethod;

public class Parent extends SuperParent {
  /**
   * JsMethod that overrides a non-JsMethod with renaming.
   */
  @Override
  @JsMethod(name = "sum")
  public int fun(int a, int b) {
    return a + b;
  }

  /**
   * JsMethod that overrides a non-JsMethod without renaming.
   */
  @Override
  @JsMethod
  public int bar(int a, int b) {
    return a * b;
  }

  /**
   * A top level JsMethod.
   */
  @JsMethod(name = "myFoo")
  public int foo(int a) {
    return a;
  }
}
