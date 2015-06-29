package com.google.j2cl.transpiler.integration.supercallnondefault;

/**
 * Test super call of non default constructor.
 */
public class Main {
  public static void main(String[] args) {
    Child child = new Child(10);
    assert child.valueInChild == 10;
    assert child.valueInParent == 10;
  }
}
