package com.google.j2cl.transpiler.regression.compiler.package1;

import com.google.j2cl.transpiler.regression.compiler.package3.SomePackageConfusedParent;

/** Class with just a package private method that returns its name. */
public class SomeParentParent extends SomePackageConfusedParent {

  // A package private method that is not live.
  String f() {
    return "Method not live";
  }

  @Override
  String m() {
    return "SomeParentParent";
  }
}
