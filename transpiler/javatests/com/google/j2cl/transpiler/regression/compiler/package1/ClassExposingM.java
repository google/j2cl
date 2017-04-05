package com.google.j2cl.transpiler.regression.compiler.package1;

import com.google.j2cl.transpiler.regression.compiler.package3.SomeInterface;

/** This class overrides a package private method an exposes it as public. */
public class ClassExposingM extends SomeParentParent implements SomeInterface {

  /**
   * This method overrides SomeParentParent.f() and exposes it as public. SomeParentParent.f() is
   * package private but never referred to except after being made public here; that makes f() dead
   * and we should not need a package private name in optimized compiles.
   */
  @Override
  public String f() {
    return "live at ClassExposingM";
  }

  @Override
  public String m() {
    return "ClassExposingM";
  }
}
