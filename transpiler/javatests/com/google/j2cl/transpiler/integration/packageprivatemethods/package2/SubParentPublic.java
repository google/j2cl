package com.google.j2cl.transpiler.integration.packageprivatemethods.package2;

import com.google.j2cl.transpiler.integration.packageprivatemethods.package1.Parent;

public class SubParentPublic extends Parent {
  // does not override Parent.fun().
  public String fun() {
    return "SubParentPublic";
  }

  // does not override Parent.bar().
  public int bar(int a) {
    return a + 5;
  }

  // does not override Parent.foo().
  @Override
  public int foo(int a, int b) {
    return a + b + 5;
  }
}
