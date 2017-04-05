package com.google.j2cl.transpiler.regression.compiler.package1;

/**
 * Class in the same package as the {@link SomeParentParent} to be able to call the package private
 * method.
 */
public class Caller {
  /**
   * This method should always call the method m() from the {@link SomeParentParent}. It should
   * never trigger a call to the one in the {@link SomeSubClassInAnotherPackage} subclass because it
   * is not actually overriding it.
   */
  public String callPackagePrivatem(SomeParentParent someClass) {
    return someClass.m();
  }
}
