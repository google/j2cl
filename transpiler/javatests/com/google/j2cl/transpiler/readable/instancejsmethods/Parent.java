package com.google.j2cl.transpiler.readable.instancejsmethods;

import jsinterop.annotations.JsMethod;

public class Parent extends SuperParent {
  /**
   * JsMethod that exposes non-JsMethod in parent, with renaming.
   *
   * <p>A bridge method m_fun__int__int should be generated.
   */
  @Override
  @JsMethod(name = "sum")
  public int fun(int a, int b) {
    return a + b;
  }

  /**
   * JsMethod that exposes non-JsMethod in parent, without renaming.
   *
   * <p>A bridge method m_bar__int__int should be generated.
   */
  @Override
  @JsMethod
  public int bar(int a, int b) {
    return a * b;
  }

  /**
   * JsMethod that does not expose non-JsMethod in parent.
   *
   * <p>No bridge method should be generated.
   */
  @JsMethod(name = "myFoo")
  public int foo(int a) {
    return a;
  }
}
