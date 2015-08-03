package com.google.j2cl.transpiler.integration.packageprivatemethods.package2;

import com.google.j2cl.transpiler.integration.packageprivatemethods.package1.Child;

public class SubChild extends Child {
  // overrides SuperParent.fun()
  @Override
  public String fun() {
    return "SubChild";
  }

  //overrides SuperParent.bar()
  @Override
  public int bar(int a) {
    return a + 2;
  }

  //overrides SuperParent.foo()
  @Override
  public int foo(int a, int b) {
    return a + b + 3;
  }
}
