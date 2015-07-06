package com.google.j2cl.transpiler.integration.cyclicclinits;

class Parent {
  static {
    // Parent.$clinit() executes before Child.$clinit(), so Child.countInChild has not been
    // initialized.
    assert Child.countInChild == 0;
  }
}

public class Child extends Parent {
  public static int countInChild = 10;

  public static void test() {
    // Child.$clinit() has been called, so countInChild has been initialized to 10.
    assert countInChild == 10;
  }
}
