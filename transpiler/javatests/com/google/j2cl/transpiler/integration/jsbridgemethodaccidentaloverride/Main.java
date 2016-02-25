package com.google.j2cl.transpiler.integration.jsbridgemethodaccidentaloverride;

public class Main {
  public static void main(String... args) {
    MyJsType a = new MyJsType();
    SubJsType b = new SubJsType();
    MyInterface c = new SubJsType();
    OtherInterface d = new SubJsType();
    assert a.foo(1) == 1;
    assert b.foo(1) == 1;
    assert c.foo(1) == 1;
    assert a.bar(1) == 2;
    assert b.bar(1) == 3;
    assert c.bar(1) == 3;
    assert a.fun(1) == 0;
    assert b.fun(1) == 0;
    assert d.fun(1) == 0;
    assert a.toString().startsWith(a.getClass().getName());
  }
}
