package com.google.j2cl.transpiler.integration.packageprivatemethods.package1;

public class SuperParent {
  String fun() {
    return "SuperParent";
  }

  // exposed by Child.bar()
  int bar(int a) {
    return a;
  }

  // exposed by Parent.foo()
  int foo(int a, int b) {
    return a + b;
  }
}
