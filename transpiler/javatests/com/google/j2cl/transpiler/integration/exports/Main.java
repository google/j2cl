package com.google.j2cl.transpiler.integration.exports;

public class Main {
  public static void main(String[] args) {
    // Will fail if the library that provides Foo is not available.
    Object foo = new Foo();
    assert foo instanceof Foo;

    // Will fail if the library that provides Bar is not available.
    Object bar = new Bar();
    assert bar instanceof Bar;
  }
}
