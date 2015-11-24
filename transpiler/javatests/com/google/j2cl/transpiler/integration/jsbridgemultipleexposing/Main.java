package com.google.j2cl.transpiler.integration.jsbridgemultipleexposing;

public class Main {
  public static void main(String... args) {
    A a = new Foo();
    B b = new Foo();
    I i = new Foo();
    Foo f = new Foo();

    assert (a.m() == 10);
    assert (b.m() == 10);
    assert (i.m() == 10);
    assert (f.m() == 10);
    assert (new A().m() == 1);
    assert (new B().m() == 5);
  }
}
