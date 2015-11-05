package com.google.j2cl.transpiler.integration.depsshallow;

import com.google.j2cl.transpiler.integration.depsshallow.foo.Foo;

public class Main {
  public static void main(String... args) {
    Foo foo = new Foo();
    assert foo.getName().equals("Foo");
  }
}
