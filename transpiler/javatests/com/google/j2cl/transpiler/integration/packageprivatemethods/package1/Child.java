package com.google.j2cl.transpiler.integration.packageprivatemethods.package1;

public class Child extends Parent {
  // exposes Parent.fun().
  @Override
  public String fun() {
    return "Child";
  }

  // exposes SuperParent.fun();
  @Override
  public int bar(int a) {
    return a + 1;
  }

  // does not expose any methods, overrides SuperParent.foo()
  @Override
  public int foo(int a, int b) {
    return a + b + 2;
  }
}
