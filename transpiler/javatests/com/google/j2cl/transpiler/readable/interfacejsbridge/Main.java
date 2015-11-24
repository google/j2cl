package com.google.j2cl.transpiler.readable.interfacejsbridge;

public class Main {
  public void test() {
    MyJsInterface a = new InterfaceImpl();
    MyInterface b = new InterfaceImpl();
    SubInterface c = new InterfaceImpl();
    InterfaceImpl d = new InterfaceImpl();
    a.foo(1);
    b.foo(1);
    c.foo(1);
    d.foo(1);
  }
}
