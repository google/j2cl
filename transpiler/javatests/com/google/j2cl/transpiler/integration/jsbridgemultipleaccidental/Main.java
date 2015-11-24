package com.google.j2cl.transpiler.integration.jsbridgemultipleaccidental;

public class Main {
  public static void main(String... args) {
    InterfaceOne a = new Test();
    InterfaceTwo b = new Test();
    InterfaceThree c = new Test();
    C d = new Test();
    Test e = new Test();

    assert (a.fun(1) == 1);
    assert (b.fun(1) == 1);
    assert (c.fun(1) == 1);
    assert (d.fun(1) == 1);
    assert (e.fun(1) == 1);
  }
}
