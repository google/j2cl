package com.google.j2cl.transpiler.integration.cyclicclinits;

class Parent {
  static {
    // Parent.$clinit() executes before Child.$clinit(), so fieldInChild has not been initialized.
    assert Child.fieldInChild == null;
  }
}

public class Child extends Parent {
  public static Object fieldInChild = new Object();

  public static void test() {
    // Child.$clinit() has been called, so fieldInChild has been initialized.
    assert fieldInChild != null;
  }
}
