package com.google.j2cl.transpiler.integration.jsbridgemethodaccidentaloverride;

import jsinterop.annotations.JsConstructor;

public class SubJsType extends MyJsType implements MyInterface {
  @JsConstructor
  public SubJsType() {}

  // There should be bridge method for MyInterface.foo() because it is accidentally overridden
  // by MyJsType.foo().

  /**
   * There should be a bridge method for MyInterface.bar() because it is exposed by this bar().
   */
  @Override
  public int bar(int a) {
    return a + 2;
  }

  // There should no be a bridge method for OtherInterface.fun() because there is already one in
  // MyJsType.
}
