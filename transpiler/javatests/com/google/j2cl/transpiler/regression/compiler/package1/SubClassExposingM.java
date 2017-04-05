package com.google.j2cl.transpiler.regression.compiler.package1;

/**
 * A subclass that replaces m() in both contexts (if called through SomeInterface or through
 * SomeParentParentParent) .
 */
public class SubClassExposingM extends ClassExposingM {

  @Override
  public String m() {
    return "SubClassExposingM";
  }
}
