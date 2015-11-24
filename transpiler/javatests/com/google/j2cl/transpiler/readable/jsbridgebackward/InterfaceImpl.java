package com.google.j2cl.transpiler.readable.jsbridgebackward;

import jsinterop.annotations.JsType;

@JsType
interface MyJsInterface {
  int foo(int a);
}

class A {
  public int foo(int a) {
    return a;
  }
}

public class InterfaceImpl extends A implements MyJsInterface {
  // A bridge method from MyJsInterface.foo to A.foo__int should be generated.
}
