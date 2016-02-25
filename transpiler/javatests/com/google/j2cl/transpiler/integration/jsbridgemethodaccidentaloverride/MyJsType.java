package com.google.j2cl.transpiler.integration.jsbridgemethodaccidentaloverride;

import jsinterop.annotations.JsType;

@JsType
public class MyJsType implements OtherInterface {
  /**
   * Accidentally exposes non-JsMethod MyInterface.foo(). Thus there should be a bridge method in
   * SubJsType.
   */
  public int foo(int a) {
    return a;
  }

  /**
   * Does not accidentally exposes non-JsMethod MyInterface.bar() because SubJsType exposes it. A
   * bridge method should still be generated in SubJsType.
   */
  public int bar(int a) {
    return a + 1;
  }

  /**
   * Exposes non-JsMethod OtherInterface.fun(). There should be a bridge method in MyJsType. There
   * should not be a bridge method in SubJsType.
   */
  @Override
  public int fun(int a) {
    return a - 1;
  }

  // toString method is also accidentally overriden which is a @JsMethod.
}
