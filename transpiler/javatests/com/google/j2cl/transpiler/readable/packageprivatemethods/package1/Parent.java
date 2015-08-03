package com.google.j2cl.transpiler.readable.packageprivatemethods.package1;

public class Parent extends SuperParent {
  // This is directly exposed by Child
  @Override
  int foo(int a) {
    return a;
  }

  // This directly exposes SuperParent.bar, not is exposed by Child.
  // There should be a dispatch method for it.
  @Override
  public int bar(int a, int b, int c) {
    return a + b + c + 1;
  }
}
