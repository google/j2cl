package com.google.j2cl.transpiler.integration.packageprivatemethods.package2;

import com.google.j2cl.transpiler.integration.packageprivatemethods.package1.Parent;

public class SubParentPP extends Parent {
  // does not override SuperParent.fun()
  String fun() {
    return "SubParentPP";
  }

  // does not override SuperParent.bar()
  int bar(int a) {
    return a + 4;
  }

  // Overrides SuperParent.foo().
  @Override
  public int foo(int a, int b) {
    return a + b + 4;
  }
}
