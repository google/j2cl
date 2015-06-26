package com.google.j2cl.transpiler.readable.arraysimple;

public class ArraySimple {
  public void main() {
    // Creation
    Object[] objects = new Object[100];
    Object[][] objects2d = new Object[5][10];
    Object[][] objectsPartial = new Object[20][];

    // Access
    Object object = objects[0];
    object = objects2d[0][1];

    // Assignment
    objects[0] = null;
    objects2d[0][1] = null;

    // There's no compound assignment for Object types.
  }
}
