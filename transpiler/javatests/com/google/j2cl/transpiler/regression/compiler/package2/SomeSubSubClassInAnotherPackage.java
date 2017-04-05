package com.google.j2cl.transpiler.regression.compiler.package2;

/**
 * Subclass in another package, with a protected method with the same name as a package private one
 * in the super class.
 */
public class SomeSubSubClassInAnotherPackage extends SomeSubClassInAnotherPackage {
  @Override
  protected String m() {
    return "SomeSubSubClassInAnotherPackage";
  }
}
