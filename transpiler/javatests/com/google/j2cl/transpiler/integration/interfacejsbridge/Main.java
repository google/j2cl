package com.google.j2cl.transpiler.integration.interfacejsbridge;

public class Main {
  public static void main(String... args) {
    MyJsInterface a = new InterfaceImpl();
    MyInterface b = new InterfaceImpl();
    SubInterface c = new InterfaceImpl();
    InterfaceImpl d = new InterfaceImpl();
    assert (a.foo(1) == 1);
    assert (b.foo(2) == 2);
    assert (c.foo(3) == 3);
    assert (d.foo(4) == 4);
  }
}
