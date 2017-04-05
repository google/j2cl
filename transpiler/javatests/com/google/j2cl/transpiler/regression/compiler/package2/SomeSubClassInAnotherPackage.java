package com.google.j2cl.transpiler.regression.compiler.package2;

import com.google.j2cl.transpiler.regression.compiler.package1.SomeParent;

/**
 * Subclass in another package, with a protected method with the same name as a package private one
 * in the super class.
 */
public class SomeSubClassInAnotherPackage extends SomeParent {
  /**
   * For this method, eclipse is giving me the warning "The method SomeSubClassInAnotherPackage.m()
   * does not override the inherited method from SomeParentClass since it is private to a different
   * package".
   */
  protected String m() {
    return "SomeSubClassInAnotherPackage";
  }

  /** A public way to manually call the method above. */
  public static String pleaseCallm(SomeSubClassInAnotherPackage obj) {
    return obj.m();
  }
}
