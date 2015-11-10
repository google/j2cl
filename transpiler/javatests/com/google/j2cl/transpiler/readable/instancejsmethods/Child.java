package com.google.j2cl.transpiler.readable.instancejsmethods;

import jsinterop.annotations.JsMethod;

public class Child extends Parent implements MyInterface {
  /**
   * Non-JsMethod that overrides a JsMethod with renaming.
   *
   * <p>It should be generated as sum() (non-mangled name), no bridge method should be generated
   * because the bridge method is already generated in its parent.
   */
  @Override
  public int fun(int a, int b) {
    return a + b + 1;
  }

  /**
   * Non-JsMethod that overrides a JsMethod without renaming.
   *
   * <p>It should be generated as bar() (non-mangled name), no bridge method should be generated
   * because the bridge method is already generated in its parent.
   */
  @Override
  public int bar(int a, int b) {
    return a * b + 1;
  }

  /**
   * JsMethod that overrides a JsMethod.
   *
   * <p>No bridge method should be generated.
   */
  @Override
  @JsMethod(name = "myFoo")
  public int foo(int a) {
    return a;
  }

  /**
   * JsMethod that overrides a non-JsMethod from its interface.
   *
   * <p> A bridge method should be generated.
   */
  @Override
  @JsMethod
  public int intfFoo() {
    return 1;
  }
}
