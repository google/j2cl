package com.google.j2cl.transpiler.regression.compiler.package1;

/** Class with just a package private method that returns its name. */
public class SomeParentParentParent {

  String m() {
    return "SomeParentParentParent";
  }

  public static String callSomeParentParentParentM(SomeParentParentParent obj) {
    return obj.m();
  }
}
