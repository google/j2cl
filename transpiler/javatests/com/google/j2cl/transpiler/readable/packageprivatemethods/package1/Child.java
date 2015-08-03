package com.google.j2cl.transpiler.readable.packageprivatemethods.package1;

public class Child extends Parent {
  // This directly exposes Parent.foo, there should be one and only one dispatch method here.
  @Override
  public int foo(int a) {
    return a + 1;
  }

  // This directly exposes SuperParent.fun, there should be a dispatch method here.
  @Override
  public int fun(int a, int b) {
    return a + b + 1;
  }

  // This does not directly expose any methods.
  @Override
  public int bar(int a, int b, int c) {
    return a + b + c + 2;
  }
}
