package com.google.j2cl.transpiler.integration.instancejsmethods;

import jsinterop.annotations.JsMethod;

public class Child extends Parent implements MyInterface {
  /**
   * Non-JsMethod that overrides a JsMethod with renaming.
   */
  @Override
  public int fun(int a, int b) {
    return a + b + 1;
  }

  /**
   * Non-JsMethod that overrides a JsMethod without renaming.
   */
  @Override
  public int bar(int a, int b) {
    return a * b + 1;
  }

  /**
   * JsMethod that overrides a JsMethod.
   */
  @Override
  @JsMethod(name = "myFoo")
  public int foo(int a) {
    return a + 1;
  }

  /**
   * JsMethod that overrides a non-JsMethod from its interface.
   */
  @Override
  @JsMethod
  public int intfFoo(int a) {
    return a;
  }
}
