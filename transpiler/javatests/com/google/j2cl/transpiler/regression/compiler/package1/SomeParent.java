package com.google.j2cl.transpiler.regression.compiler.package1;

/** Class overriding a package private method that returns its name. */
public class SomeParent extends SomeParentParent {

  @Override
  String m() {
    return "SomeParent";
  }
}
