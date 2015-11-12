package com.google.j2cl.transpiler.readable.interfacewithfields;

public class Main {
  public void test() {
    int a = MyInterface.a + MyInterface.b;
    MyInterface intf = new MyInterface() {};
    a = intf.a + intf.b;
  }
}
