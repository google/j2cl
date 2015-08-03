package com.google.j2cl.transpiler.integration.packageprivatemethods.package1;

public class Parent extends SuperParent {
  // overrides SuperParent.fun(), and is exposed by Child.fun().
  @Override
  String fun() {
    return "Parent";
  }

  @Override
  public int foo(int a, int b) {
    return a + b + 1;
  }
}
