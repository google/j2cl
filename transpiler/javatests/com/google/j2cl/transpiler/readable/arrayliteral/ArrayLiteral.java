package com.google.j2cl.transpiler.readable.arrayliteral;

public class ArrayLiteral {
  @SuppressWarnings("unused")
  public void main() {
    Object object = new Object();

    int[] ints = new int[] {0, 1, 2};
    Object[][] objects2d = new Object[][] {{object, object}, {object, object}};
    int[][] partial = new int[][] {};
    Object[][] unbalanced = new Object[][] {{object, object}, null};
  }
}
