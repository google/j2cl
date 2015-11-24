package com.google.j2cl.transpiler.readable.jsbridgeaccidentaloverride;

public class Main {
  public void test() {
    MyJsType a = new MyJsType();
    SubJsType b = new SubJsType();
    MyInterface c = new SubJsType();
    OtherInterface d = new SubJsType();
    a.foo(1);
    b.foo(1);
    c.foo(1);
    a.bar(1);
    b.bar(1);
    c.bar(1);
    a.fun(1);
    b.fun(1);
    d.fun(1);
  }
}
