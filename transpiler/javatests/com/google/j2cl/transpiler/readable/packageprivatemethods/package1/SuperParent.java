package com.google.j2cl.transpiler.readable.packageprivatemethods.package1;

public class SuperParent {
  // This is not directly exposed by any sub class.
  int foo(int a) {
    return a - 1;
  }

  // This is directly exposed by Child
  int fun(int a, int b) {
    return a + b;
  }

  // This is directly exposed by Parent
  int bar(int a, int b, int c) {
    return a + b + c;
  }
}
