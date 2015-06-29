package com.google.j2cl.transpiler.integration.supercallnondefault;

public class Child extends Parent {
  public int valueInChild;

  public Child(int value) {
    super(value);
    this.valueInChild = value;
  }
}
