package com.google.j2cl.transpiler.readable.devirtualizedcalls;

public class ArrayCalls {
  public void main() {
    Object[][] array2d = new Object[1][1];

    int length1 = array2d[0].length;
    int length2 = array2d.length;
  }
}
