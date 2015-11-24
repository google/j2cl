package com.google.j2cl.transpiler.integration.jsbridgebackward;

public class Main {
  public static void main(String... args) {
    MyJsInterface a = new InterfaceImpl();
    A b = new InterfaceImpl();
    InterfaceImpl c = new InterfaceImpl();

    assert (a.foo(1) == 1);
    assert (b.foo(1) == 1);
    assert (c.foo(1) == 1);
  }
}
