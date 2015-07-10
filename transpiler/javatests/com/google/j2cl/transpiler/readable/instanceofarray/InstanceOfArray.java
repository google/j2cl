package com.google.j2cl.transpiler.readable.instanceofarray;

public class InstanceOfArray {
  @SuppressWarnings("unused")
  public void main() {
    Object object = new Object();

    boolean a = object instanceof Object[];
    boolean b = object instanceof Object[][];
  }
}
