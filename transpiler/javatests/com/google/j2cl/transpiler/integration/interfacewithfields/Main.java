package com.google.j2cl.transpiler.integration.interfacewithfields;

public class Main {
  public static void main(String... args) {
    assert MyInterface.a == 1;
    assert MyInterface.b == 2;
    MyInterface intf = new MyInterface() {};
    assert intf.a == 1;
    assert intf.b == 2;
  }
}
